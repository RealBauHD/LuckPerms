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

package me.lucko.luckperms.sculk;

import io.github.sculkpowered.server.Server;
import io.github.sculkpowered.server.entity.player.Player;
import io.github.sculkpowered.server.event.EventOrder;
import io.github.sculkpowered.server.event.Subscribe;
import io.github.sculkpowered.server.event.lifecycle.ServerInitializeEvent;
import io.github.sculkpowered.server.event.lifecycle.ServerShutdownEvent;
import io.github.sculkpowered.server.plugin.Plugin;
import io.github.sculkpowered.server.plugin.PluginDescription;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.luckperms.api.platform.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Sculk.
 */
@PluginDescription(
        name = "LuckPerms",
        version = "@version@"
)
public class LPSculkBootstrap extends Plugin implements LuckPermsBootstrap {

    /**
     * The plugin logger
     */
    private PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPSculkPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    public LPSculkBootstrap() {
        this.schedulerAdapter = new SculkSchedulerAdapter(this);
        this.classPathAppender = new SculkClassPathAppender(this);
        this.plugin = new LPSculkPlugin(this);
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    // lifecycle

    @Subscribe(order = EventOrder.FIRST)
    public void onEnable(ServerInitializeEvent event) {
        this.logger = new Slf4jPluginLogger(this.logger());
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Subscribe(order = EventOrder.LAST)
    public void onDisable(ServerShutdownEvent e) {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.SCULK;
    }

    @Override
    public String getServerBrand() {
        return "Sculk";
    }

    @Override
    public String getServerVersion() {
        return Server.class.getPackage().getImplementationVersion();
    }

    @Override
    public Path getDataDirectory() {
        return Paths.get("plugins", "LuckPerms").toAbsolutePath();
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(this.server().player(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return Optional.empty();
    }

    @Override
    public int getPlayerCount() {
        return this.server().playerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<? extends Player> players = this.server().onlinePlayers();
        List<String> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.name());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<? extends Player> players = this.server().onlinePlayers();
        List<UUID> list = new ArrayList<>(players.size());
        for (Player player : players) {
            list.add(player.uniqueId());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = this.server().player(uniqueId);
        return player != null;
    }
}
