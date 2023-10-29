/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sculk.listeners;

import io.github.sculkpowered.server.entity.player.Player;
import io.github.sculkpowered.server.event.EventOrder;
import io.github.sculkpowered.server.event.Subscribe;
import io.github.sculkpowered.server.event.player.PlayerDisconnectEvent;
import io.github.sculkpowered.server.event.player.PlayerInitialEvent;
import io.github.sculkpowered.server.event.player.PlayerJoinEvent;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.sculk.LPSculkPlugin;
import me.lucko.luckperms.sculk.service.CompatibilityUtil;
import me.lucko.luckperms.sculk.util.AdventureCompat;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SculkConnectionListener extends AbstractConnectionListener {
    private final LPSculkPlugin plugin;

    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public SculkConnectionListener(LPSculkPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Subscribe
    public void handle(PlayerInitialEvent event) {
        final Player player = event.player();
        if (this.deniedLogin.remove(player.uniqueId())) {
            player.disconnect((Component) AdventureCompat.toPlatformComponent(TranslationManager
                    .render(Message.LOADING_DATABASE_ERROR.build(), player.settings().locale())));
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + player.uniqueId() + " - " + player.name());
        }

        try {
            User user = loadUser(player.uniqueId(), player.name());
            recordConnection(player.uniqueId());
            event.permissionChecker(new PermissionCheckerImpl(user, player));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.uniqueId(), player.name(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.uniqueId() + " - " + player.name(), ex);
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                this.deniedLogin.add(player.uniqueId());
            }
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.uniqueId(), player.name(), null);
        }
    }

    @Subscribe
    public void onPlayerPostLogin(PlayerJoinEvent event) {
        final Player player = event.player();
        final User user = this.plugin.getUserManager().getIfLoaded(player.uniqueId());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.uniqueId() + " - " + player.name());
        }

        if (user == null) {
            if (!getUniqueConnections().contains(player.uniqueId())) {
                this.plugin.getLogger().warn("User " + player.uniqueId() + " - " + player.name() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + player.uniqueId() + " - " + player.name() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // disconnect the user
                player.disconnect(TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.settings().locale()));
            } else {
                // just send a message
                this.plugin.getBootstrap().getScheduler().asyncLater(() -> {
                    Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
                }, 1, TimeUnit.SECONDS);
            }
        }
    }

    @Subscribe(order = EventOrder.LAST)
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        handleDisconnect(event.player().uniqueId());
    }

    private class PermissionCheckerImpl implements PermissionChecker {

        private final User user;
        private final Player player;

        public PermissionCheckerImpl(final User user, final Player player) {
            this.user = user;
            this.player = player;
        }

        @Override
        public @NotNull TriState value(@NotNull String permission) {
            return CompatibilityUtil.convertTristate(this.user.getCachedData()
                    .getPermissionData(SculkConnectionListener.this.plugin.getContextManager()
                            .getCacheFor(this.player).getQueryOptions())
                    .checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result());
        }
    }
}
