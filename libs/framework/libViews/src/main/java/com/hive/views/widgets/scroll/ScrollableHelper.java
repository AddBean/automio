// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.scroll;


import android.annotation.SuppressLint;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ScrollView;

public class ScrollableHelper {
    private ScrollableContainer mCurrentScrollableCainer;

    private int sysVersion = android.os.Build.VERSION.SDK_INT;

    private static boolean isRecyclerViewTop(RecyclerView recyclerView) {

        if (recyclerView != null) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                if (firstVisibleItemPosition < 0) {
                    return true;
                }
                View childAt = recyclerView.getChildAt(0);
                if (childAt == null) {
                    return true;
                }

                int headerCount = 0;
                //拿到header的个数
                if (recyclerView instanceof RecyclerView) {
                    RecyclerView lRecyclerView = (RecyclerView) recyclerView;
                    RecyclerView.Adapter adapter = (RecyclerView.Adapter) lRecyclerView.getAdapter();
//                    headerCount = adapter.getHeaderViewsCount() + adapter.getRefresHeaderCount();
                }

                if (firstVisibleItemPosition <= headerCount) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) childAt.getLayoutParams();
                    int topMargin = lp.topMargin;
                    int top = childAt.getTop();
                    if (top >= topMargin) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isAdapterViewTop(AdapterView adapterView) {
        if (adapterView != null) {
            int firstVisiblePosition = adapterView.getFirstVisiblePosition();
            View childAt = adapterView.getChildAt(0);
            if (childAt == null || (firstVisiblePosition == 0 && childAt != null && childAt.getTop() == 0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isScrollViewTop(ScrollView scrollView) {
        if (scrollView != null) {
            int scrollViewY = scrollView.getScrollY();
            return scrollViewY <= 0;
        }
        return false;
    }

    private static boolean isWebViewTop(WebView scrollView) {
        if (scrollView != null) {
            int scrollViewY = scrollView.getScrollY();
            return scrollViewY <= 0;
        }
        return false;
    }

    public ScrollableContainer getCurrentScrollableContainer() {
        return mCurrentScrollableCainer;
    }

    public void setCurrentScrollableContainer(ScrollableContainer scrollableContainer) {
        this.mCurrentScrollableCainer = scrollableContainer;
    }

    private View getScrollableView() {
        return mCurrentScrollableCainer != null ? mCurrentScrollableCainer.getScrollableView() : null;
    }

    public boolean isTop() {
        View scrollableView = getScrollableView();
        if (scrollableView == null) {
            return false;
        }

        if (scrollableView instanceof AdapterView) {
            return isAdapterViewTop((AdapterView) scrollableView);
        }
        if (scrollableView instanceof ScrollView) {
            return isScrollViewTop((ScrollView) scrollableView);
        }
        if (scrollableView instanceof RecyclerView) {
            return isRecyclerViewTop((RecyclerView) scrollableView);
        }
        if (scrollableView instanceof WebView) {
            return isWebViewTop((WebView) scrollableView);
        }
        throw new IllegalStateException("scrollableView must be a instance of AdapterView|ScrollView|RecyclerView");
    }

    @SuppressLint("NewApi")
    public void smoothScrollBy(int velocityY, int distance, int duration) {
        View scrollableView = getScrollableView();
        if (scrollableView instanceof AbsListView) {
            AbsListView absListView = (AbsListView) scrollableView;
            if (sysVersion >= 21) {
                absListView.fling(velocityY);
            } else {
                absListView.smoothScrollBy(distance, duration);
            }
        } else if (scrollableView instanceof ScrollView) {
            ((ScrollView) scrollableView).fling(velocityY);
        } else if (scrollableView instanceof RecyclerView) {
            ((RecyclerView) scrollableView).fling(0, velocityY);
        } else if (scrollableView instanceof WebView) {
            ((WebView) scrollableView).flingScroll(0, velocityY);
        }
    }

    /**
     * a viewgroup whitch contains ScrollView/ListView/RecycelerView..
     */
    public interface ScrollableContainer {
        /**
         * @return ScrollView/ListView/RecycelerView..'s instance
         */
        View getScrollableView();

        boolean canScroll();

        String getScrollableViewId();
    }
}
