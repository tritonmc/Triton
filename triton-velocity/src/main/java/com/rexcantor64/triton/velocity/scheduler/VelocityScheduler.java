package com.rexcantor64.triton.velocity.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class VelocityScheduler implements TritonScheduler<Void, Void> {
    private final @NotNull ProxyServer server;
    private final @NotNull Object plugin;

    @Override
    public @NotNull TaskHandler runGlobal(@NotNull Runnable task) {
        val handler = server.getScheduler().buildTask(plugin, task).schedule();
        return new VelocityTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runGlobalLater(@NotNull Runnable task, long delayTicks) {
        val handler = server.getScheduler().buildTask(plugin, task)
                .delay(delayTicks * 50, TimeUnit.MILLISECONDS)
                .schedule();
        return new VelocityTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSync(@Nullable Void ignore, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runSyncLater(@Nullable Void ignore, @NotNull Runnable task, long delayTicks) {
        return this.runGlobalLater(task, delayTicks);
    }

    @Override
    public @NotNull TaskHandler runSyncAtLocation(@Nullable Void ignore, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runAsync(@NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    @Contract("_, _ -> false")
    public boolean isSyncThread(@Nullable Void ignore1, @Nullable Void ignore2) {
        return false; // there's no sync thread in velocity
    }

    @Override
    @Contract("_ -> false")
    public boolean isThreadOwnedByPlayer(@Nullable Void ignore) {
        return false; // there's no sync thread in velocity
    }

    @Override
    @Contract("_ -> false")
    public boolean isThreadOwnedByLocation(@Nullable Void ignore) {
        return false; // there's no sync thread in velocity
    }

    @RequiredArgsConstructor
    public static class VelocityTaskHandler implements TaskHandler {
        private final @NotNull ScheduledTask task;

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }
}
