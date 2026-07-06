package com.rexcantor64.triton.spigot.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
public class BukkitScheduler implements TritonScheduler<Player, Location> {
    private final @NotNull Plugin plugin;

    @Override
    public @NotNull TaskHandler runGlobal(@NotNull Runnable task) {
        val handler = Bukkit.getScheduler().runTask(this.plugin, task);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runGlobalLater(@NotNull Runnable task, long delayTicks) {
        val handler = Bukkit.getScheduler().runTaskLater(this.plugin, task, delayTicks);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSync(@Nullable Player ignore, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runSyncLater(@Nullable Player ignore, @NotNull Runnable task, long delayTicks) {
        return this.runGlobalLater(task, delayTicks);
    }

    @Override
    public @NotNull TaskHandler runSyncAtLocation(@Nullable Location ignore, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runAsync(@NotNull Runnable task) {
        val handler = Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
        return new BukkitTaskHandler(handler);
    }

    @Override
    public boolean isSyncThread(@Nullable Player ignore1, @Nullable Location ignore2) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isThreadOwnedByPlayer(@NonNull Player player) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isThreadOwnedByLocation(@NonNull Location location) {
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
