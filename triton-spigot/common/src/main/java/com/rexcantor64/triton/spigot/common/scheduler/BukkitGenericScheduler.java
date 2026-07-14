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

    /**
     * Run a task in the sync thread owned by the given entity.
     * Depending on the platform, this might run on the main sync thread instead.
     *
     * @param entity The entity owning the thread this task should be run on.
     * @param task The task to run in the sync thread.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    public abstract TaskHandler runSync(Entity entity, Runnable task);

    /**
     * Run a task in the sync thread owned by the given location.
     * Depending on the platform, this might run on the main sync thread instead.
     *
     * @param location The location owning the thread this task should be run on.
     * @param task The task to run in the sync thread.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    public abstract TaskHandler runSync(Location location, Runnable task);

    /**
     * Run a task in the sync thread owned by the given entity after a given delay in server ticks.
     * There are 20 ticks in a second.
     *
     * @param entity The entity owning the thread this task should be run on.
     * @param task The task to run in the sync thread.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    public abstract TaskHandler runSyncLater(Entity entity, Runnable task, long delayTicks);

    /**
     * Run a computation on a sync/main thread and obtain its result.
     *
     * @param entity If applicable, use this entity to select the sync thread to run the computation on.
     * @param task   The computation to run on the sync thread.
     * @param <T>    The return type of the computation.
     * @return The result of the computation, or an empty optional if it returned null or failed.
     * @since 4.1.0
     */
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

    /**
     * Whether the current thread is synchronous.
     * If both parameters are null, only checks if this is the main thread.
     *
     * @param entity   If given, also check whether the entity owns the current thread.
     * @param location If given, also check whether the location owns the current thread.
     * @return true if the current thread is the main thread or owned by one of the given parameters, false otherwise.
     * @since 4.1.0
     */
    public abstract boolean isSyncThread(@Nullable Entity entity, @Nullable Location location);

    /**
     * Whether the current thread is owned by the given entity.
     *
     * @param entity The entity to check thread ownership against.
     * @return true if the given entity owns the current thread, false otherwise.
     * @since 4.1.0
     */
    public abstract boolean isMainThreadOrOwnedBy(Entity entity);

    /**
     * Whether the current thread is owned by the given location.
     *
     * @param location The location to check thread ownership against.
     * @return true if the given location owns the current thread, false otherwise.
     * @since 4.1.0
     */
    public abstract boolean isMainThreadOrOwnedBy(Location location);

}
