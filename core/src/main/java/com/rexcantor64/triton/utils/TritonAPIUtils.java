package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.TritonAPI;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Utility functions to easily register the {@link Triton} instance in
 * {@link TritonAPI} for consumption by other plugins.
 *
 * @since 4.0.0
 */
public class TritonAPIUtils {
    private static final Method REGISTER;

    static {
        try {
            Class<?> apiClass = Class.forName("com.rexcantor64.triton.api.TritonAPIImpl");
            REGISTER = apiClass.getDeclaredMethod("register", Triton.class, boolean.class);
            REGISTER.setAccessible(true);

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to initialize Triton API", e);
        }
    }

    public static void register(@NotNull Triton<?, ?> instance, boolean adventureRelocated) {
        try {
            REGISTER.invoke(null, instance, adventureRelocated);
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to initialize Triton API");
        }
    }

}
