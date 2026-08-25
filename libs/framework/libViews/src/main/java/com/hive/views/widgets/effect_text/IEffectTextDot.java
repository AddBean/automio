// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.effect_text;

import android.graphics.Canvas;

/**
 * Created by AddBean on 2016/10/20.
 */

public interface IEffectTextDot {
    void onDraw(Canvas canvas);
    void evolveDot(float value);
}