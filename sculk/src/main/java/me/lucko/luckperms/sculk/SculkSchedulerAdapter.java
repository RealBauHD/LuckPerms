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

import io.github.sculkpowered.server.scheduler.Scheduler;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.util.Iterators;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class SculkSchedulerAdapter implements SchedulerAdapter {

    private final LPSculkBootstrap bootstrap;

    private final Executor executor;
    private final Set<Scheduler.Task> tasks = Collections.newSetFromMap(new WeakHashMap<>());

    public SculkSchedulerAdapter(LPSculkBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.executor = r -> bootstrap.getServer().getScheduler().newTask(bootstrap, r)
                .delay(0, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public Executor async() {
        return this.executor;
    }

    @Override
    public Executor sync() {
        return this.executor;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        Scheduler.Task t = this.bootstrap.getServer().getScheduler().newTask(this.bootstrap, task)
                .delay((int) delay, unit);
        t.schedule();
        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        Scheduler.Task t = this.bootstrap.getServer().getScheduler().newTask(this.bootstrap, task)
                .delay((int) interval, unit)
                .repeat((int) interval, unit);
        t.schedule();
        this.tasks.add(t);
        return t::cancel;
    }

    @Override
    public void shutdownScheduler() {
        Iterators.tryIterate(this.tasks, Scheduler.Task::cancel);
    }

    @Override
    public void shutdownExecutor() {
        // do nothing
    }
}
