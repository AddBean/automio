// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Key;

import java.security.MessageDigest;

/**
 * Created by gangzhiguo
 * on 2018/6/2
 */

public class BbDataCacheKey implements Key {
    private final Key sourceKey;
    private final Key signature;

    BbDataCacheKey(Key sourceKey, Key signature) {
        this.sourceKey = sourceKey;
        this.signature = signature;
    }

    Key getSourceKey() {
        return sourceKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BbDataCacheKey) {
            BbDataCacheKey other = (BbDataCacheKey) o;
            return sourceKey.equals(other.sourceKey) && signature.equals(other.signature);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = sourceKey.hashCode();
        result = 31 * result + signature.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataCacheKey{"
                + "sourceKey=" + sourceKey
                + ", signature=" + signature
                + '}';
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        sourceKey.updateDiskCacheKey(messageDigest);
        signature.updateDiskCacheKey(messageDigest);
    }
}
