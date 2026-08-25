// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/9/21
 */

interface IOperateMenuInterface {

    fun getOperateMenuContainerView():ViewGroup

    fun getOperateFragmentManager(): FragmentManager
}