// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.device;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by kg007 on 2018/5/10.
 */

class BuildProperties {
    private static BuildProperties ourInstance;
    private Properties properties;

    static BuildProperties getInstance() throws IOException {
        if (ourInstance == null) {
            ourInstance = new BuildProperties();
        }
        return ourInstance;
    }

    private BuildProperties() throws IOException {
        FileInputStream inputStream = null;
        try {
            properties = new Properties();
            File file = new File(Environment.getRootDirectory(), "build.prop");
            inputStream = new FileInputStream(file);
            properties.load(inputStream);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public boolean containsKey(final Object key) {
        return properties == null ? false : properties.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return properties == null ? false : properties.containsValue(value);
    }

    public String getProperty(final String name) {
        return properties == null ? "" : properties.getProperty(name);
    }

    public String getProperty(final String name, final String defaultValue) {
        return properties == null ? "" : properties.getProperty(name, defaultValue);
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return properties.entrySet();
    }

    public boolean isEmpty() {
        return properties == null || properties.isEmpty();
    }

    public Enumeration keys() {
        return properties == null ? null : properties.keys();
    }

    public Set keySet() {
        return properties == null ? null : properties.keySet();
    }

    public int size() {
        return properties == null ? 0 : properties.size();
    }

    public Collection values() {
        return properties == null ? null : properties.values();
    }
}
