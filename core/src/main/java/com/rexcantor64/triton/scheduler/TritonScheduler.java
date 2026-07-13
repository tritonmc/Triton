package com.rexcantor64.triton.scheduler;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * Schedule tasks to run synchronously or asynchronously, or after a delay.
 *
 * @since 4.1.0
 */
@NotNullByDefault
public interface TritonScheduler {

    TaskHandler runSync(Runnable task);

    TaskHandler runSyncLater(Runnable task, long delayTicks);

    TaskHandler runAsync(Runnable task);

    interface TaskHandler {
        void cancel();
    }
}
