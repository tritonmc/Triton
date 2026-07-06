package com.rexcantor64.triton.scheduler;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public interface TritonScheduler<P, L> {

    @NotNull TaskHandler runGlobal(@NotNull Runnable task);

    @NotNull TaskHandler runGlobalLater(@NotNull Runnable task, long delayTicks);

    @NotNull TaskHandler runSync(@NotNull P player, @NotNull Runnable task);

    @NotNull TaskHandler runSyncLater(@NotNull P player, @NotNull Runnable task, long delayTicks);

    default @NotNull <T> Optional<T> callSync(@NotNull P player, @NotNull Callable<T> task) {
        try {
            if (this.isThreadOwnedByPlayer(player)) {
                return Optional.ofNullable(task.call());
            }
            val future = new FutureTask<T>(task);
            this.runSync(player, future);
            return Optional.ofNullable(future.get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @NotNull TaskHandler runSyncAtLocation(@NotNull L location, @NotNull Runnable task);

    default @NotNull <T> Optional<T> callSyncAtLocation(@NotNull L location, @NotNull Callable<T> task) {
        try {
            if (this.isThreadOwnedByLocation(location)) {
                return Optional.ofNullable(task.call());
            }
            val future = new FutureTask<T>(task);
            this.runSyncAtLocation(location, future);
            return Optional.ofNullable(future.get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @NotNull TaskHandler runAsync(@NotNull Runnable task);

    boolean isSyncThread(@Nullable P player, @Nullable L location);

    boolean isThreadOwnedByPlayer(@NotNull P player);

    boolean isThreadOwnedByLocation(@NotNull L location);

    interface TaskHandler {
        void cancel();
    }
}
