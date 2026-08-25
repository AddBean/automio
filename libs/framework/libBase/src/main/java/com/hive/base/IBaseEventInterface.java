// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base;

import com.hive.adapter.core.AbsCardItemView;

public interface IBaseEventInterface {
    void onCardEvent(int cardEvent, Object args, AbsCardItemView itemView);
}
