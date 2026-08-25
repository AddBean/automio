// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

import android.content.Context;

public interface ICardItemFactory<D extends CardItemData, P extends ICardItemView> {

    P createItemView(Context context, int type);

    int offerTypeCount();
}
