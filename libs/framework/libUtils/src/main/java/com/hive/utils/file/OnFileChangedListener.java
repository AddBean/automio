// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.file;

import java.io.File;

public interface OnFileChangedListener {
    public void onChanged(File f);

    public boolean isStoped();
}