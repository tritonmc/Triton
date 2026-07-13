package com.rexcantor64.triton.spigot.scheduler;

import com.rexcantor64.triton.spigot.common.scheduler.BukkitGenericScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public class BukkitScheduler extends BukkitGenericScheduler {

    public BukkitScheduler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public TaskHandler runSync(Runnable task) {
        val handler = Bukkit.getScheduler().runTask(this.plugin, task);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public TaskHandler runSyncLater(Runnable task, long delayTicks) {
        val handler = Bukkit.getScheduler().runTaskLater(this.plugin, task, delayTicks);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public TaskHandler runSync(@Nullable Entity ignore, Runnable task) {
        return this.runSync(task);
    }

    @Override
    public TaskHandler runSync(@Nullable Location ignore, Runnable task) {
        return this.runSync(task);
    }

    @Override
    public TaskHandler runSyncLater(@Nullable Entity ignore, Runnable task, long delayTicks) {
        return this.runSyncLater(task, delayTicks);
    }

    @Override
    public TaskHandler runAsync(Runnable task) {
        val handler = Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public boolean isSyncThread(@Nullable Entity ignore1, @Nullable Location ignore2) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isMainThreadOrOwnedBy(Entity ignore) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isMainThreadOrOwnedBy(Location ignore) {
        return Bukkit.isPrimaryThread();
    }

    @RequiredArgsConstructor
    public static class BukkitTaskHandler implements TaskHandler {
        private final @Nullable BukkitTask task;

        @Override
        public void cancel() {
            if (this.task != null) {
                this.task.cancel();
            }
        }
    }
}
