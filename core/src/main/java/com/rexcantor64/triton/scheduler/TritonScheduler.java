package com.rexcantor64.triton.scheduler;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * Schedule tasks to run synchronously or asynchronously, or after a delay.
 *
 * @since 4.1.0
 */
@NotNullByDefault
public interface TritonScheduler {

    /**
     * Run a task in the server's sync/main thread.
     *
     * @param task The task to run in the sync thread.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    TaskHandler runSync(Runnable task);

    /**
     * Run a task in the server's sync/main thread after a given delay in server ticks.
     * There are 20 ticks in a second.
     *
     * @param task The task to run in the sync thread.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    TaskHandler runSyncLater(Runnable task, long delayTicks);

    /**
     * Run a task in an async thread.
     *
     * @param task The task to run asynchronously.
     * @return A reference to the scheduled task.
     * @since 4.1.0
     */
    TaskHandler runAsync(Runnable task);

    /**
     * A reference to a scheduled task.
     *
     * @since 4.1.0
     */
    interface TaskHandler {
        /**
         * Cancel the scheduled task.
         * This method might be a no-op if the task cannot be canceled (e.g., it has already finished).
         *
         * @since 4.1.0
         */
        void cancel();
    }
}
