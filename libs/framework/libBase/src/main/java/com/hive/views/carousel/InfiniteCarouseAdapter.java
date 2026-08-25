// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.carousel;

import android.view.View;

import com.hive.adapter.core.CardItemData;
import com.hive.adapter.core.ICardItemView;

public abstract class InfiniteCarouseAdapter {
    protected void onItemSelected(View v, CardItemData data) {
    }

    protected void onItemEvent(int cardEvent, Object args) {

    }


    protected void onItemPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }


    protected abstract ICardItemView getCardView(CardItemData itemData);
}
