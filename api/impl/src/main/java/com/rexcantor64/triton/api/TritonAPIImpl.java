package com.rexcantor64.triton.api;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.impl.adventure.TritonImpl;
import lombok.val;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.InvocationTargetException;

public class TritonAPIImpl {

    /**
     * Register the Triton instance to be accessible from the external API.
     *
     * @param instance           The Triton instance currently loaded.
     * @param adventureRelocated Whether the adventure API has been relocated, so that a conversion layer can be put in place.
     */
    @SuppressWarnings("unused")
    @ApiStatus.Internal
    public static void register(Triton<?, ?> instance, boolean adventureRelocated) {
        // This method is called via reflection in TritonAPIUtils
        if (adventureRelocated) {
            register(new TritonImpl(instance));
        } else {
            register(instance);
        }
    }

    private static void register(com.rexcantor64.triton.api.Triton instance) {
        // Due to different class loaders, we cannot call TritonAPI#register directly
        try {
            val apiClass = Class.forName("com.rexcantor64.triton.api.TritonAPI");
            val method = apiClass.getDeclaredMethod("register", com.rexcantor64.triton.api.Triton.class);
            method.setAccessible(true);

            method.invoke(null, instance);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
