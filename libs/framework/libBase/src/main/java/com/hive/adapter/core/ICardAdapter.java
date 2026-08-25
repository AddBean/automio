// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

import java.util.List;

public interface ICardAdapter {
    void setFactory(ICardItemFactory factory);

    void setData(List<CardItemData> data);
}
