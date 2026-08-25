// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.TextView;

import com.hive.markdown.MarkdownMarkwon;
import com.hive.plugin.provider.IEditorProvider;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.editorGeneratedDatabaseHolder;

import io.noties.markwon.Markwon;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/1/21
 */
public class EditorProvider implements IEditorProvider {
    @Override
    public void init(Context context) {
        FlowManager.initModule(editorGeneratedDatabaseHolder.class);
    }

    @Override
    public void renderMarkdown(TextView textView, String markdown) {
        Markwon markwon = MarkdownMarkwon.INSTANCE.create(textView.getContext(), textView);
        markwon.setMarkdown(textView, markdown);
    }

    @Override
    public Bitmap renderMarkdownToBitmap(String markdown) {
        return MarkdownMarkwon.INSTANCE.renderToBitmapSync(markdown);
    }
}
