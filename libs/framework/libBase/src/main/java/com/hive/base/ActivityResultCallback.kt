// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base

import android.content.Intent

interface ActivityResultCallback {
    abstract fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    )

    abstract fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}