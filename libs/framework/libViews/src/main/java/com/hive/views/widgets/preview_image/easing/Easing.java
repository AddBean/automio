// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.preview_image.easing;

public interface Easing {

	double easeOut(double time, double start, double end, double duration);

	double easeIn(double time, double start, double end, double duration);

	double easeInOut(double time, double start, double end, double duration);
}
