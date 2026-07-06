package com.rexcantor64.triton.bungeecord.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BungeeScheduler implements TritonScheduler<Void, Void> {
    private final @NotNull Plugin plugin;

    @Override
    public @NotNull TaskHandler runGlobal(@NotNull Runnable task) {
        task.run();
        return new BungeeTaskHandler(null);
    }

    @Override
    public @NotNull TaskHandler runGlobalLater(@NotNull Runnable task, long delayTicks) {
        val handler = this.plugin.getProxy().getScheduler().schedule(this.plugin, task, delayTicks * 50, TimeUnit.MILLISECONDS);
        return new BungeeTaskHandler(handler);
    }

    @Override
    public @NotNull TaskHandler runSync(@Nullable Void player, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runSyncLater(@Nullable Void player, @NotNull Runnable task, long delayTicks) {
        return this.runGlobalLater(task, delayTicks);
    }

    @Override
    public @NotNull TaskHandler runSyncAtLocation(@Nullable Void ignore, @NotNull Runnable task) {
        return this.runGlobal(task);
    }

    @Override
    public @NotNull TaskHandler runAsync(@NotNull Runnable task) {
        val handler = this.plugin.getProxy().getScheduler().runAsync(this.plugin, task);
        return new BungeeTaskHandler(handler);
    }

    @Override
    @Contract("_, _ -> false")
    public boolean isSyncThread(@Nullable Void ignore1, @Nullable Void ignore2) {
        return false; // there's no sync thread in bungee
    }

    @Override
    @Contract("_ -> false")
    public boolean isThreadOwnedByPlayer(@Nullable Void ignore) {
        return false; // there's no sync thread in bungee
    }

    @Override
    @Contract("_ -> false")
    public boolean isThreadOwnedByLocation(@Nullable Void ignore) {
        return false; // there's no sync thread in bungee
    }

    @RequiredArgsConstructor
    public static class BungeeTaskHandler implements TaskHandler {
        private final @Nullable ScheduledTask task;

        @Override
        public void cancel() {
            if (this.task != null) {
                this.task.cancel();
            }
        }
    }

}
