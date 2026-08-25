// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ComponentManager {
    private static Map<Class, IComponentProvider> sProvider = new HashMap<>();
    private static ComponentManager sInstance;

    public static ComponentManager getInstance() {
        synchronized (ComponentManager.class) {
            if (null == sInstance) {
                synchronized (ComponentManager.class) {
                    sInstance = new ComponentManager();
                }
            }
        }
        return sInstance;
    }

    public  <T extends IComponentProvider> IComponentProvider getProvider(Class<T> clazz) {
        synchronized (sProvider){
            for (Class clazzKey : sProvider.keySet()) {
                Type[] types = clazzKey.getGenericInterfaces();
                for (int i = 0; i < types.length; i++) {
                    if (types[i].equals(clazz)) {
                        return sProvider.get(clazzKey);
                    }
                }
            }
            return null;
        }
    }

    public void register(String componentClass) {
        try {
            Class clazz = Class.forName(componentClass);
            IComponentProvider instance = (IComponentProvider) clazz.newInstance();
            instance.init(GlobalApp.getContext());
            register(instance);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    public <T extends IComponentProvider> void register(T component) {
        synchronized (sProvider){
            sProvider.put(component.getClass(), component);
        }
    }

    public <T extends IComponentProvider> void unregister(T component) {
        synchronized (sProvider){
            sProvider.remove(component.getClass());
        }
    }
}
