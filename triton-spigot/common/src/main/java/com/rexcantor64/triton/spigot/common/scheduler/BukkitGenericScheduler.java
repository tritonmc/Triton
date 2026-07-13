package com.rexcantor64.triton.spigot.common.scheduler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.scheduler.TritonScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@RequiredArgsConstructor
@NotNullByDefault
public abstract class BukkitGenericScheduler implements TritonScheduler {
    protected final Plugin plugin;

    public abstract TaskHandler runSync(Entity entity, Runnable task);

    public abstract TaskHandler runSync(Location location, Runnable task);

    public abstract TaskHandler runSyncLater(Entity entity, Runnable task, long delayTicks);


    public <T> Optional<T> callSync(Entity entity, Callable<@Nullable T> task) {
        try {
            if (this.isMainThreadOrOwnedBy(entity)) {
                return Optional.ofNullable(task.call());
            }
            val future = new FutureTask<@Nullable T>(task);
            this.runSync(entity, future);
            return Optional.ofNullable(future.get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to run callable in sync thread");
            return Optional.empty();
        }
    }

    public <T> Optional<T> callSync(Location location, Callable<@Nullable T> task) {
        try {
            if (this.isMainThreadOrOwnedBy(location)) {
                return Optional.ofNullable(task.call());
            }
            val future = new FutureTask<@Nullable T>(task);
            this.runSync(location, future);
            return Optional.ofNullable(future.get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to run callable in sync thread");
            return Optional.empty();
        }
    }

    public abstract boolean isSyncThread(@Nullable Entity entity, @Nullable Location location);

    public abstract boolean isMainThreadOrOwnedBy(Entity entity);

    public abstract boolean isMainThreadOrOwnedBy(Location location);

}
