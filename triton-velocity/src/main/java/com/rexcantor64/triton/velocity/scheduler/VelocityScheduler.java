package com.rexcantor64.triton.velocity.scheduler;

import com.rexcantor64.triton.scheduler.TritonScheduler;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@NotNullByDefault
public class VelocityScheduler implements TritonScheduler {
    private final ProxyServer server;
    private final Object plugin;

    @Override
    public TaskHandler runSync(Runnable task) {
        val handler = server.getScheduler().buildTask(plugin, task).schedule();
        return new VelocityTaskHandler(handler);
    }

    @Override
    public TaskHandler runSyncLater(Runnable task, long delayTicks) {
        val handler = server.getScheduler().buildTask(plugin, task)
                .delay(delayTicks * 50, TimeUnit.MILLISECONDS)
                .schedule();
        return new VelocityTaskHandler(handler);
    }

    @Override
    public TaskHandler runAsync(Runnable task) {
        return this.runSync(task);
    }

    @RequiredArgsConstructor
    public static class VelocityTaskHandler implements TaskHandler {
        private final ScheduledTask task;

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }
}
