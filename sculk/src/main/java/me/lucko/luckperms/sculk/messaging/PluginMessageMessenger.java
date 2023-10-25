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

package me.lucko.luckperms.sculk.messaging;

import io.github.sculkpowered.server.MinecraftServer;
import io.github.sculkpowered.server.event.Subscribe;
import io.github.sculkpowered.server.event.connection.PluginMessageEvent;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.sculk.LPSculkPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger extends AbstractPluginMessageMessenger {
    private static final String CHANNEL = AbstractPluginMessageMessenger.CHANNEL;

    private final LPSculkPlugin plugin;

    public PluginMessageMessenger(LPSculkPlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        MinecraftServer server = this.plugin.getBootstrap().getServer();
        server.getEventHandler().register(this.plugin.getBootstrap(), this);
    }

    @Override
    public void close() {
        MinecraftServer server = this.plugin.getBootstrap().getServer();
        server.getEventHandler().unregister(this.plugin.getBootstrap(), this);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        final MinecraftServer server = this.plugin.getBootstrap().getServer();
        if (server.getPlayerCount() != 0) {
            server.getAllPlayers().iterator().next().sendPluginMessage(CHANNEL, buf);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        this.handleIncomingMessage(event.getData());
    }
}
