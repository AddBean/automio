// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

import com.hive.utils.global.CommonUtilsWrapper;
import com.hive.utils.system.CommonUtils;
import com.hive.utils.utils.IntentUtils;

public class AutoLinkTextView extends AppCompatTextView {
    public AutoLinkTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public AutoLinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public AutoLinkTextView(Context context) {
        super(context);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        SpannableString span = new SpannableString(getText());
        ClickableSpan[] links = span.getSpans(getSelectionStart(),
                getSelectionEnd(), ClickableSpan.class);
        try {
            super.onTouchEvent(event);
            if (links.length != 0) {
                return true;
            }
            return false;
        } catch (AndroidRuntimeException e) {

            if (links.length > 0) {
                if (links[0] instanceof URLSpan) {
                    CommonUtils.startDefaultBrowser(getContext(), ((URLSpan) links[0]).getURL());
                    return true;
                }
            }
            return false;
        }
    }
}