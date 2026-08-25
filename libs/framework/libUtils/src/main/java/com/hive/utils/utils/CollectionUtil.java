// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CollectionUtil {

    public static <V> void foreach(Collection<V> l, Callback<V> callback) {
        if (l == null) return;
        for (V v : l) {
            callback.onCall(v);
        }
    }

    public interface Merger<V, T> {
        T getId(V v);
    }

    public interface MergeSorter<V, T> extends Merger<V, T>, Comparator<V> {
    }

    public interface Filter<V> {
        boolean filter(V v);
    }

    public interface Converter<V, T> {
        T convert(V v);
    }

    public interface Callback<V> {
        void onCall(V v);
    }

    public static <V, T> List<T> convert(Collection<V> l, Converter<V, T> converter) {
        if (l == null)
            return null;

        List<T> result = new ArrayList<T>(l.size());
        for (V v : l) {
            result.add(converter.convert(v));
        }

        return result;
    }

    public static <V, T> V search(Collection<V> l, V v, Merger<V, T> finder) {
        if (l == null)
            return null;

        final T f = finder.getId(v);
        if (f == null)
            return null;

        for (V g : l) {
            if (f.equals(finder.getId(g)))
                return g;
        }

        return null;
    }

    public static <V, T> V searchById(Collection<V> l, T t, Merger<V, T> finder) {
        if (l == null)
            return null;

        final T f = t;
        if (f == null)
            return null;

        for (V g : l) {
            if (f.equals(finder.getId(g)))
                return g;
        }

        return null;
    }

    public static <V, T> int find(List<V> l, V v, Merger<V, T> finder) {
        final T f = finder.getId(v);
        if (f == null)
            return -1;

        for (int i = 0; i < l.size(); i++) {
            if (f.equals(finder.getId(l.get(i))))
                return i;
        }

        return -1;
    }

    public static <V>  List<V>  filter(List<V> l, Filter<V> filter) {
        List<V> ls=new ArrayList<>();
        for (int i = 0; i <l.size() ; i++) {
            if(filter.filter(l.get(i))){
                ls.add(l.get(i));
            }
        }
        return ls;
    }

    public static <V, T> List<V> merge(List<V> dst, List<V> src, Merger<V, T> merger) {
        if (dst == null || src == null || merger == null)
            return dst;

        for (V s : src) {
            final int idx = find(dst, s, merger);
            if (idx < 0) {
                dst.add(s);
            } else {
                dst.set(idx, s);
            }
        }

        return dst;
    }

    public static <V, T> List<V> mergeSort(List<V> dst, List<V> src, MergeSorter<V, T> mergeSorter) {
        if (dst == null || src == null || mergeSorter == null) {
            if (dst != null)
                Collections.sort(dst, mergeSorter);
            return dst;
        }

        for (V s : src) {
            final int idx = find(dst, s, mergeSorter);
            if (idx < 0) {
                dst.add(s);
            } else {
                dst.set(idx, s);
            }
        }

        Collections.sort(dst, mergeSorter);
        return dst;
    }

    public static int compare(long l, long r) {
        if (l < r)
            return -1;
        if (l > r)
            return 1;
        return 0;
    }

    public static int size(Collection<?> cs) {
        return cs != null ? cs.size() : 0;
    }

    public static boolean empty(Collection<?> cs) {
        return size(cs) <= 0;
    }

    public static <T> List<T> limit(Collection<T> cs, int limit) {
        if (cs == null)
            return null;

        List<T> result = new ArrayList<T>(limit);
        for (T t : cs) {
            if (result.size() >= limit)
                break;

            result.add(t);
        }

        return result;
    }

    public static <T> List<T> copy(List<T> ts) {
        if (ts == null)
            return null;

        List<T> result = new ArrayList<T>(ts.size());
        for (T t : ts) {
            result.add(t);
        }

        return result;
    }

    public static boolean equals(Object l, Object r) {
        return l == null ? l == r : l.equals(r);
    }

    public static boolean isValidIndex(Collection c, int index) {
        return c != null && index >= 0 && index < c.size();
    }


    public static <T> List<List<T>> group(Collection<T> data, Comparator<? super T> c) {
        List<List<T>> result = new ArrayList<>();
        for (T t : data) {
            boolean isSameGroup = false;
            for (List<T> aResult : result) {
                if (c.compare(t, aResult.get(0)) == 0) {
                    isSameGroup = true;
                    aResult.add(t);
                    break;
                }
            }
            if (!isSameGroup) {
                List<T> innerList = new ArrayList();
                innerList.add(t);
                result.add(innerList);
            }
        }
        return result;
    }
}
