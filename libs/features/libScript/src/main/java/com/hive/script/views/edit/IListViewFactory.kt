// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

/**
 *
 * @author jiadou
 * @date 4/22/21
 */
interface IListViewFactory {
    fun createItemView(viewType: Int): AbsListItemView
}