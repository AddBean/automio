// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

import android.view.View;

import com.hive.base.IBaseEventInterface;
import com.hive.base.IBaseListInterface;

import java.util.List;

public interface ICardItemView<P> {
    View getView();

    void bindData(P data);

    void onPayload(List payloads);

    void setBaseListImpl(IBaseListInterface listImpl);

    void setBaseEventImpl(IBaseEventInterface eventImpl);



}
