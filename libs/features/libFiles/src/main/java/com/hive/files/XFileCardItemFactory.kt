// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.Context
import com.hive.adapter.core.CardItemData
import com.hive.adapter.core.ICardItemFactory
import com.hive.adapter.core.ICardItemView
import com.hive.files.card.*

/**
 *
 * @author jiadou
 * @date 4/7/21
 */
class XFileCardItemFactory : ICardItemFactory<CardItemData, ICardItemView<CardItemData>> {

    override fun createItemView(context: Context, type: Int): ICardItemView<CardItemData>? {
        return when (type) {
            Card_Type_Folder -> XFileFolderCard(context)
            Card_Type_File -> XFileFileCard(context)
            Card_Type_Grid_Folder -> XFileFolderGridCard(context)
            Card_Type_Grid_File -> XFileFileGridCard(context)
            Card_Type_PREVIEW_IMAGE -> XPreviewImageCard(context)
            else -> null
        }
    }

    override fun offerTypeCount(): Int = 0

    companion object {
        val Card_Type_Folder = 1000
        val Card_Type_File = 1001
        val Card_Type_Grid_Folder = 1002
        val Card_Type_Grid_File = 1003
        val Card_Type_PREVIEW_IMAGE = 1004
        val instance: XFileCardItemFactory by lazy { XFileCardItemFactory() }
    }

}