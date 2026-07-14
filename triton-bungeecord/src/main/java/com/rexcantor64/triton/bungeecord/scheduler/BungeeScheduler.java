package com.rexcantor64.triton.bungeecord.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@NotNullByDefault
public class BungeeScheduler implements TritonScheduler {
    private final Plugin plugin;

    @Override
    public TaskHandler runSync(Runnable task) {
        task.run();
        return new BungeeTaskHandler(null);
    }

    @Override
    public TaskHandler runSyncLater(Runnable task, long delayTicks) {
        val handler = this.plugin.getProxy().getScheduler().schedule(this.plugin, task, delayTicks * 50, TimeUnit.MILLISECONDS);
        return new BungeeTaskHandler(handler);
    }

    @Override
    public TaskHandler runAsync(Runnable task) {
        val handler = this.plugin.getProxy().getScheduler().runAsync(this.plugin, task);
        return new BungeeTaskHandler(handler);
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
