package com.rexcantor64.triton.spigot.folia.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
public class FoliaScheduler implements TritonScheduler<Player, Location> {
    private final @NotNull Plugin plugin;

    @Override
    public @NotNull TaskHandler runGlobal(@NotNull Runnable task) {
        val handler = Bukkit.getGlobalRegionScheduler().run(this.plugin, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runGlobalLater(@NotNull Runnable task, long delayTicks) {
        val handler = Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, ignore -> task.run(), delayTicks);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSync(@NotNull Player player, @NotNull Runnable task) {
        val handler = player.getScheduler().run(this.plugin, ignore -> task.run(), null);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSyncLater(@NotNull Player player, @NotNull Runnable task, long delayTicks) {
        val handler = player.getScheduler().runDelayed(this.plugin, ignore -> task.run(), null, delayTicks);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSyncAtLocation(@NonNull Location location, @NotNull Runnable task) {
        val handler = Bukkit.getRegionScheduler().run(this.plugin, location, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runAsync(@NotNull Runnable task) {
        val handler = Bukkit.getAsyncScheduler().runNow(this.plugin, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public boolean isSyncThread(@Nullable Player player, @Nullable Location location) {
        return Bukkit.isPrimaryThread()
                || Bukkit.isGlobalTickThread()
                || (player != null && this.isThreadOwnedByPlayer(player))
                || (location != null && this.isThreadOwnedByLocation(location));
    }

    @Override
    public boolean isThreadOwnedByPlayer(@NonNull Player player) {
        return Bukkit.isOwnedByCurrentRegion(player);
    }

    @Override
    public boolean isThreadOwnedByLocation(@NonNull Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    @RequiredArgsConstructor
    public static class FoliaTaskHandler implements TaskHandler {
        private final @Nullable ScheduledTask foliaTask;

        @Override
        public void cancel() {
            if (this.foliaTask != null) {
                this.foliaTask.cancel();
            }
        }
    }
}