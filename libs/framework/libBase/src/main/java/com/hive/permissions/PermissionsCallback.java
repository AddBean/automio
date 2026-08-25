// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.permissions;

import java.util.List;

/**
 * Created by Administrator on 2017/7/18.
 */

public interface  PermissionsCallback {
     void onDenied(List<String> lackedPermissions);

     void onGranted();
}
