package com.rexcantor64.triton.utils;

import com.google.common.base.Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

    public static Object getMethod(Object target, String methodName) {
        return getMethod(target, methodName, new Class[0], new Object[0]);
    }

    public static Object getMethod(Object target, String methodName, Class<?>[] paramTypes,
                                   Object[] params) {
        Preconditions.checkNotNull(target, "Target is null");
        Preconditions.checkNotNull(methodName, "Method name is null");

        Class<?> currentClazz = target.getClass();
        Object returnValue = null;
        do {
            try {
                Method method = currentClazz.getDeclaredMethod(methodName, paramTypes);
                returnValue = method.invoke(target, params);
            } catch (Exception exception) {
                currentClazz = currentClazz.getSuperclass();
            }

        } while ((currentClazz != null) && (currentClazz.getSuperclass() != null) && (returnValue == null));

        return returnValue;
    }

    public static Object getField(Object object, String fieldName) {
        try {
            return object.getClass().getField(fieldName).get(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getStaticField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getDeclaredField(Object object, String fieldName) {
        return getDeclaredField(object, fieldName, false);
    }

    public static Object getDeclaredField(Object object, String fieldName, boolean isStatic) {
        Class<?> c = object.getClass();
        Object returnValue = null;
        do {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                if (isStatic)
                    returnValue = f.get(null);
                returnValue = f.get(object);
                c = c.getSuperclass();
            } catch (Exception exception) {
                c = c.getSuperclass();
            }
        } while ((c != null) && (c.getSuperclass() != null) && (returnValue == null));
        return returnValue;
    }

    public static void setDeclaredField(Object object, String fieldName, Object fieldValue) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(object, fieldValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPublicFinalField(Object object, String fieldName, Object newValue) {
        try {
            Field f = object.getClass().getField(fieldName);
            f.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);

            f.set(object, newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPrivateFinalField(Object object, String fieldName, Object newValue) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);

            f.set(object, newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cloneFields(Object from, Object to) {
        try {
            for (Field f : from.getClass().getFields()) {
                try {
                    f.set(to, f.get(from));
                } catch (Exception e) {
                }
            }
            for (Field f : from.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    f.set(to, f.get(from));
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Class<?> getClassOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
