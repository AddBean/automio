// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.config

import com.hive.utils.utils.BaseSPClass
import java.io.Serializable

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/8/21
 */

class XFileConfig : BaseSPClass(), Serializable {
    public var inGrid = false
    public var gridCount = 4
    public var sortType = 0

    companion object {
        val instance: XFileConfig by lazy { XFileConfig() }
        var sConfigCache: XFileConfig? = null
    }

    override fun getSaveName(): String = javaClass.simpleName
}