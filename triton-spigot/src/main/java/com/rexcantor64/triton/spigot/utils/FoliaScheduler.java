package com.rexcantor64.triton.spigot.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public final class FoliaScheduler {
    public static final TaskHandle NOOP_TASK = () -> {
    };

    private static final Class<?>[] NO_ARGS = new Class<?>[0];
    private static volatile @Nullable Boolean modernSchedulers;

    private FoliaScheduler() {
    }

    public static boolean hasModernSchedulers() {
        Boolean cached = modernSchedulers;
        if (cached != null) {
            return cached;
        }

        boolean detected = hasMethod(Bukkit.getServer().getClass(), "getGlobalRegionScheduler")
                && hasMethod(Bukkit.getServer().getClass(), "getAsyncScheduler")
                && hasMethod(Bukkit.getServer().getClass(), "getRegionScheduler");
        modernSchedulers = detected;
        return detected;
    }

    public static boolean isGlobalTickThread() {
        Method method = findMethod(Bukkit.class, "isGlobalTickThread");
        if (method == null) {
            return Bukkit.isPrimaryThread();
        }

        try {
            return (boolean) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            return Bukkit.isPrimaryThread();
        }
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        Method method = findMethod(Bukkit.class, "isOwnedByCurrentRegion", Entity.class);
        if (method == null) {
            return Bukkit.isPrimaryThread();
        }

        try {
            return (boolean) method.invoke(null, entity);
        } catch (ReflectiveOperationException e) {
            return Bukkit.isPrimaryThread();
        }
    }

    public static boolean isOwnedByCurrentRegion(Location location) {
        Method method = findMethod(Bukkit.class, "isOwnedByCurrentRegion", Location.class);
        if (method == null) {
            return Bukkit.isPrimaryThread();
        }

        try {
            return (boolean) method.invoke(null, location);
        } catch (ReflectiveOperationException e) {
            return Bukkit.isPrimaryThread();
        }
    }

    public static void runAsync(JavaPlugin plugin, Runnable runnable) {
        if (hasModernSchedulers()) {
            try {
                Object scheduler = getAsyncScheduler();
                invoke(
                        scheduler,
                        "runNow",
                        new Class[]{Plugin.class, Consumer.class},
                        plugin,
                        consumer(runnable)
                );
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runGlobal(JavaPlugin plugin, Runnable runnable) {
        if (hasModernSchedulers()) {
            if (isGlobalTickThread()) {
                runnable.run();
                return;
            }

            try {
                Object scheduler = getGlobalRegionScheduler();
                invoke(
                        scheduler,
                        "execute",
                        new Class[]{Plugin.class, Runnable.class},
                        plugin,
                        runnable
                );
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static TaskHandle runGlobalLater(JavaPlugin plugin, Runnable runnable, long delayTicks) {
        if (delayTicks <= 0L) {
            runGlobal(plugin, runnable);
            return NOOP_TASK;
        }

        if (hasModernSchedulers()) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                Object task = invoke(
                        scheduler,
                        "runDelayed",
                        new Class[]{Plugin.class, Consumer.class, long.class},
                        plugin,
                        consumer(runnable),
                        delayTicks
                );
                return new ReflectiveTaskHandle(task);
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        return new ReflectiveTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
    }

    public static boolean runEntity(JavaPlugin plugin, Entity entity, Runnable runnable) {
        if (hasModernSchedulers()) {
            if (isOwnedByCurrentRegion(entity)) {
                runnable.run();
                return true;
            }

            try {
                Object scheduler = getEntityScheduler(entity);
                return (boolean) invoke(
                        scheduler,
                        "execute",
                        new Class[]{Plugin.class, Runnable.class, Runnable.class, long.class},
                        plugin,
                        runnable,
                        null,
                        1L
                );
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return true;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
        return true;
    }

    public static boolean runEntityLater(JavaPlugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (delayTicks <= 0L) {
            return runEntity(plugin, entity, runnable);
        }

        if (hasModernSchedulers()) {
            try {
                Object scheduler = getEntityScheduler(entity);
                return (boolean) invoke(
                        scheduler,
                        "execute",
                        new Class[]{Plugin.class, Runnable.class, Runnable.class, long.class},
                        plugin,
                        runnable,
                        null,
                        delayTicks
                );
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return true;
    }

    public static boolean runRegion(JavaPlugin plugin, Location location, Runnable runnable) {
        if (hasModernSchedulers()) {
            if (isOwnedByCurrentRegion(location)) {
                runnable.run();
                return true;
            }

            try {
                Object scheduler = getRegionScheduler();
                invoke(
                        scheduler,
                        "execute",
                        new Class[]{Plugin.class, Location.class, Runnable.class},
                        plugin,
                        location,
                        runnable
                );
                return true;
            } catch (ReflectiveOperationException ignored) {
                // Fallback below
            }
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return true;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
        return true;
    }

    public static <T> Optional<T> callGlobal(JavaPlugin plugin, Callable<T> callable) {
        try {
            if (hasModernSchedulers()) {
                if (isGlobalTickThread()) {
                    return Optional.ofNullable(callable.call());
                }

                CompletableFuture<T> future = new CompletableFuture<>();
                runGlobal(plugin, () -> completeFuture(future, callable));
                return Optional.ofNullable(await(future));
            }

            if (Bukkit.isPrimaryThread()) {
                return Optional.ofNullable(callable.call());
            }

            return Optional.ofNullable(Bukkit.getScheduler().callSyncMethod(plugin, callable).get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static <T> Optional<T> callEntity(JavaPlugin plugin, Entity entity, Callable<T> callable) {
        try {
            if (hasModernSchedulers()) {
                if (isOwnedByCurrentRegion(entity)) {
                    return Optional.ofNullable(callable.call());
                }

                CompletableFuture<T> future = new CompletableFuture<>();
                Object scheduler = getEntityScheduler(entity);
                boolean scheduled = (boolean) invoke(
                        scheduler,
                        "execute",
                        new Class[]{Plugin.class, Runnable.class, Runnable.class, long.class},
                        plugin,
                        (Runnable) () -> completeFuture(future, callable),
                        (Runnable) () -> future.complete(null),
                        1L
                );
                if (!scheduled) {
                    return Optional.empty();
                }
                return Optional.ofNullable(await(future));
            }

            if (Bukkit.isPrimaryThread()) {
                return Optional.ofNullable(callable.call());
            }

            return Optional.ofNullable(Bukkit.getScheduler().callSyncMethod(plugin, callable).get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static <T> T await(CompletableFuture<T> future) throws InterruptedException, ExecutionException {
        return future.get();
    }

    private static <T> void completeFuture(CompletableFuture<T> future, Callable<T> callable) {
        try {
            future.complete(callable.call());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private static Consumer<Object> consumer(Runnable runnable) {
        return ignored -> runnable.run();
    }

    private static Object getAsyncScheduler() throws ReflectiveOperationException {
        return invoke(Bukkit.getServer(), "getAsyncScheduler", NO_ARGS);
    }

    private static Object getGlobalRegionScheduler() throws ReflectiveOperationException {
        return invoke(Bukkit.getServer(), "getGlobalRegionScheduler", NO_ARGS);
    }

    private static Object getRegionScheduler() throws ReflectiveOperationException {
        return invoke(Bukkit.getServer(), "getRegionScheduler", NO_ARGS);
    }

    private static Object getEntityScheduler(Entity entity) throws ReflectiveOperationException {
        return invoke(entity, "getScheduler", NO_ARGS);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static boolean hasMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        return findMethod(type, methodName, parameterTypes) != null;
    }

    private static @Nullable Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public interface TaskHandle {
        void cancel();
    }

    private static final class ReflectiveTaskHandle implements TaskHandle {
        private final @Nullable Object handle;

        private ReflectiveTaskHandle(@Nullable Object handle) {
            this.handle = handle;
        }

        @Override
        public void cancel() {
            if (handle == null) {
                return;
            }

            if (handle instanceof BukkitTask) {
                ((BukkitTask) handle).cancel();
                return;
            }

            try {
                invoke(handle, "cancel", NO_ARGS);
            } catch (ReflectiveOperationException ignored) {
                // Best effort only
            }
        }
    }
}
