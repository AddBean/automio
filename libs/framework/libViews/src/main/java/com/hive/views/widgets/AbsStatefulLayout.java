// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hive.anim.AnimUtils;
import com.hive.views.R;


public abstract class AbsStatefulLayout extends RelativeLayout {
    private static final String SAVED_STATE = "stateful_layout_state";
    private final String mProgressText;
    private final String mOfflineText;
    private final String mEmptyText;
    private boolean mTransparentEnable = false;

    private State mInitialState;
    private int mProgressLayoutId;
    private int mOfflineLayoutId;
    private int mEmptyLayoutId;
    protected View mContentLayout;
    protected View mProgressLayout;
    protected View mOfflineLayout;
    protected View mEmptyLayout;
    private State mState;
    private OnStateChangeListener mOnStateChangeListener;
    private boolean mProgressFadeOutEnable = false;

    public enum State {
        CONTENT(0), PROGRESS(1), OFFLINE(2), EMPTY(3);

        private final int mValue;

        public static State valueToState(int value) {
            State[] values = State.values();
            return values[value];
        }

        State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }
    }

    public interface OnStateChangeListener {
        void onStateChange(View v, State state);
    }

    public AbsStatefulLayout(Context context) {
        this(context, null);
    }

    public AbsStatefulLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsStatefulLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AbsStatefulLayout);
        int initialStateValue = typedArray.getInt(R.styleable.AbsStatefulLayout_defState, State.CONTENT.getValue());

        mInitialState = State.valueToState(initialStateValue);
        mProgressLayoutId = typedArray.getResourceId(R.styleable.AbsStatefulLayout_progressLayout, R.layout.stateful_default_progress);
        mOfflineLayoutId = typedArray.getResourceId(R.styleable.AbsStatefulLayout_offlineLayout, R.layout.stateful_default_offline);
        mEmptyLayoutId = typedArray.getResourceId(R.styleable.AbsStatefulLayout_emptyLayout, R.layout.stateful_default_empty);
        mTransparentEnable = typedArray.getBoolean(R.styleable.AbsStatefulLayout_transparent, true);

        mProgressText = typedArray.getString(R.styleable.AbsStatefulLayout_progressText);
        mOfflineText = typedArray.getString(R.styleable.AbsStatefulLayout_offlineText);
        mEmptyText = typedArray.getString(R.styleable.AbsStatefulLayout_emptyText);
        typedArray.recycle();
    }

    private void initLayout() {
        TextView emptyMsg = findViewById(R.id.tv_empty_msg);
        TextView progressMsg = findViewById(R.id.tv_progress_msg);
        TextView offlineMsg = findViewById(R.id.tv_offline_msg);
        if (emptyMsg != null && !TextUtils.isEmpty(mEmptyText)) {
            emptyMsg.setText(mEmptyText);
        }
        if (progressMsg != null && !TextUtils.isEmpty(mProgressText)) {
            progressMsg.setText(mProgressText);
        }
        if (offlineMsg != null && !TextUtils.isEmpty(mOfflineText)) {
            offlineMsg.setText(mOfflineText);
        }
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupView();
    }

    public View getContentView() {
        return this;
    }

    public void showContent() {
        setState(State.CONTENT);
    }

    public void showProgress() {
        setState(State.PROGRESS);
    }

    public void showOffline() {
        setState(State.OFFLINE);
    }

    public void showEmpty() {
        setState(State.EMPTY);
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
        if (!mTransparentEnable && mContentLayout != null) {
            mContentLayout.setVisibility(state == State.CONTENT ? View.VISIBLE : View.INVISIBLE);
        }
        setViewVisibility(mProgressLayout, state == State.PROGRESS ? View.VISIBLE : View.GONE);
        mOfflineLayout.setVisibility(state == State.OFFLINE ? View.VISIBLE : View.GONE);
        mEmptyLayout.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        if (mOnStateChangeListener != null)
            mOnStateChangeListener.onStateChange(this, state);
        onSetState(state);
    }

    /**
     * 设置视图可见
     *
     * @param view
     * @param visibility
     */
    private void setViewVisibility(final View view, final int visibility) {
        //loading等消失时使用动画
        if (visibility == GONE && mProgressFadeOutEnable && view.getVisibility() == VISIBLE) {
            view.clearAnimation();
            AnimUtils.fadeOutAnim(view, 600, new AnimUtils.AnimListener() {
                @Override
                public void onOver(View v) {
                    super.onOver(v);
                    view.setVisibility(visibility);
                }
            });
        } else {
            view.setVisibility(visibility);
        }
    }

    public void setProgressFadeOutEnable(boolean mFadeAnimEnable) {
        this.mProgressFadeOutEnable = mFadeAnimEnable;
    }

    protected abstract void onSetState(State state);

    public void setOnStateChangeListener(OnStateChangeListener l) {
        mOnStateChangeListener = l;
    }

    public void saveInstanceState(Bundle outState) {
        if (mState != null) {
            outState.putInt(SAVED_STATE, mState.getValue());
        }
    }

    public State restoreInstanceState(Bundle savedInstanceState) {
        State state = null;
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_STATE)) {
            int value = savedInstanceState.getInt(SAVED_STATE);
            state = State.valueToState(value);
            setState(state);
        }
        return state;
    }

    private void setupView() {
        if (mContentLayout == null) {


            if (isInEditMode()) {
                return;
            }
            mContentLayout = getChildCount() > 0 ? getChildAt(0) : null;
            mProgressLayout = LayoutInflater.from(getContext()).inflate(mProgressLayoutId, this, false);
            mOfflineLayout = LayoutInflater.from(getContext()).inflate(mOfflineLayoutId, this, false);
            mEmptyLayout = LayoutInflater.from(getContext()).inflate(mEmptyLayoutId, this, false);

            mProgressLayout.setVisibility(GONE);
            mOfflineLayout.setVisibility(GONE);
            mEmptyLayout.setVisibility(GONE);

            addView(mProgressLayout);
            addView(mOfflineLayout);
            addView(mEmptyLayout);

            setState(mInitialState);
            initLayout();
        }
    }

    public View getContentLayout() {
        return mContentLayout;
    }

    public View getProgressLayout() {
        return mProgressLayout;
    }

    public View getOfflineLayout() {
        return mOfflineLayout;
    }

    public View getEmptyLayout() {
        return mEmptyLayout;
    }
}
