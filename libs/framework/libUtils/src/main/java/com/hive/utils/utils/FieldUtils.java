// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


public class FieldUtils {

    public static void writeField(final Field field, final Object target, final Object value, final boolean forceAccess) throws IllegalAccessException {
        if (forceAccess && !field.isAccessible()) {
            field.setAccessible(true);
        } else {
            MemberUtils.setAccessibleWorkaround(field);
        }
        field.set(target, value);
    }

    public static Field getField(final Class<?> cls, final String fieldName) {
        return getField(cls, fieldName, true);
    }

    private static Field getField(Class<?> cls, String fieldName, final boolean forceAccess) {
        if (cls == null || fieldName == null) {
            return null;
        }
        for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                final Field field = acls.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers())) {
                    if (forceAccess) {
                        field.setAccessible(true);
                    } else {
                        continue;
                    }
                }
                return field;
            } catch (final NoSuchFieldException ex) {
            }
        }

        Field match = null;
        for (final Class<?> class1 : getAllInterfaces(cls)) {
            try {
                final Field test = class1.getField(fieldName);
                isTrue(match == null, "Reference to field %s is ambiguous relative to %s" + "; a matching field exists on two or more implemented interfaces.", fieldName, cls);
                match = test;
            } catch (final NoSuchFieldException ex) {
            }
        }
        return match;
    }


    public static List<Class<?>> getAllInterfaces(final Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final LinkedHashSet<Class<?>> interfacesFound = new LinkedHashSet<Class<?>>();
        getAllInterfaces(cls, interfacesFound);
        return new ArrayList<>(interfacesFound);
    }

    private static void getAllInterfaces(Class<?> cls, final HashSet<Class<?>> interfacesFound) {
        while (cls != null) {
            final Class<?>[] interfaces = cls.getInterfaces();

            for (final Class<?> i : interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound);
                }
            }
            cls = cls.getSuperclass();
        }
    }


    private final static void isTrue(final boolean expression, final String message, final Object... values) {
        if (expression == false) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

}
