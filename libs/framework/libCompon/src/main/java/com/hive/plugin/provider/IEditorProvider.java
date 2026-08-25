// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import android.graphics.Bitmap;
import android.widget.TextView;

import com.hive.plugin.IComponentProvider;

/**
 * @author jiadou
 * @date 5/1/21
 */
public interface IEditorProvider extends IComponentProvider {

   void renderMarkdown(TextView textView, String markdown);

   Bitmap renderMarkdownToBitmap(String markdown);
}
