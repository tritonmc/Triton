package com.rexcantor64.triton.spigot.folia.scheduler;

import com.rexcantor64.triton.spigot.common.scheduler.BukkitGenericScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public class FoliaScheduler extends BukkitGenericScheduler {

    public FoliaScheduler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public TaskHandler runSync(Runnable task) {
        val handler = Bukkit.getGlobalRegionScheduler().run(this.plugin, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public TaskHandler runSyncLater(Runnable task, long delayTicks) {
        val handler = Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, ignore -> task.run(), delayTicks);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public TaskHandler runSync(Entity entity, Runnable task) {
        val handler = entity.getScheduler().run(this.plugin, ignore -> task.run(), null);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public TaskHandler runSync(Location location, Runnable task) {
        val handler = Bukkit.getRegionScheduler().run(this.plugin, location, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public TaskHandler runSyncLater(Entity player, Runnable task, long delayTicks) {
        val handler = player.getScheduler().runDelayed(this.plugin, ignore -> task.run(), null, delayTicks);
        return new FoliaTaskHandler(handler);
    }

    @Override
    public TaskHandler runAsync(Runnable task) {
        val handler = Bukkit.getAsyncScheduler().runNow(this.plugin, ignore -> task.run());
        return new FoliaTaskHandler(handler);
    }

    @Override
    public boolean isSyncThread(@Nullable Entity player, @Nullable Location location) {
        return Bukkit.isPrimaryThread()
                || Bukkit.isGlobalTickThread()
                || (player != null && this.isMainThreadOrOwnedBy(player))
                || (location != null && this.isMainThreadOrOwnedBy(location));
    }

    @Override
    public boolean isMainThreadOrOwnedBy(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public boolean isMainThreadOrOwnedBy(Location location) {
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