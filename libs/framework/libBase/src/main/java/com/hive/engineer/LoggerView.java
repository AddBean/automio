// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hive.base.BaseLayout;
import com.hive.base.R;
import com.hive.net.engineer.EngineerConfig;
import com.hive.permissions.FloatPermissionAdapter;
import com.hive.utils.GlobalApp;
import com.hive.utils.OnClickFilteListener;
import com.hive.utils.WorkHandler;
import com.hive.utils.debug.DLog;
import com.hive.utils.debug.DLogProxyInterface;
import com.hive.utils.utils.ScreenUtils;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class LoggerView extends BaseLayout implements DLogProxyInterface, WorkHandler.IWorkHandler, View.OnClickListener {
    private static LoggerView sInstance;
    private float x;
    private float y;
    private int oldMinX;
    private int oldMinY;
    private int oldMaxHeight;
    private WindowManager.LayoutParams layoutParams;
    private ViewHolder mViewHolder;
    private WindowManager windowManager;
    private WorkHandler mHandler;
    private GestureDetector mGesture;

    static class ViewHolder {
        ImageView mIvMin;
        TextView mTvSelector;
        ImageView mIvClose;
        RelativeLayout mLayoutTop;
        LoggerMainView mLoggerView;
        ImageView mIvPause;
        ImageView mIvClear;
        LinearLayout mLayoutSelector;
        LinearLayout mLayoutMenuContent;
        RelativeLayout mLayoutMenu;
        RelativeLayout mLayoutMain;
        ImageView mIvMax;
        TextView mTvFilter;

        ViewHolder(View view) {
            mIvMin = view.findViewById(R.id.iv_min);
            mTvSelector = view.findViewById(R.id.tv_selector);
            mIvClose = view.findViewById(R.id.iv_close);
            mLayoutTop = view.findViewById(R.id.layout_top);
            mLoggerView = view.findViewById(R.id.logger_view);
            mIvPause = view.findViewById(R.id.iv_pause);
            mIvClear = view.findViewById(R.id.iv_clear);
            mLayoutSelector = view.findViewById(R.id.layout_selector);
            mLayoutMenuContent = view.findViewById(R.id.layout_menu_content);
            mLayoutMenu = view.findViewById(R.id.layout_menu);
            mLayoutMain = view.findViewById(R.id.layout_main);
            mIvMax = view.findViewById(R.id.iv_max);
            mTvFilter = view.findViewById(R.id.tv_filter);
        }
    }

    public static LoggerView getInstance() {
        if (!EngineerConfig.read().loggerOn) return null;
        if (sInstance == null)
            sInstance = new LoggerView(GlobalApp.getContext());
        return sInstance;
    }

    public LoggerView(Context context) {
        super(context);
    }

    public LoggerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoggerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        mHandler = new WorkHandler(this);
        oldMinX = ScreenUtils.getScreenWidth()/2 - 36 * DP;
        oldMinY = ScreenUtils.getScreenHeight()/2 - 36 * DP;
        oldMaxHeight = 280 * DP;
        initSelector();
        mViewHolder.mLoggerView.setLoggerView(this);
        mViewHolder.mTvSelector.setOnClickListener(this);
        mViewHolder.mIvClose.setOnClickListener(this);
        mViewHolder.mIvPause.setOnClickListener(this);
        mViewHolder.mIvMin.setOnClickListener(this);
        mViewHolder.mIvClear.setOnClickListener(this);
        mViewHolder.mIvPause.setSelected(false);
        mViewHolder.mLayoutMenu.setOnClickListener(this);
        mViewHolder.mIvMax.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onMinTouchEvent(event);
            }
        });
        mViewHolder.mLayoutTop.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTopTouchEvent(event);
            }
        });
        mViewHolder.mTvFilter.setOnClickListener(this);
        mGesture = new GestureDetector(new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                layoutParams.x = 0;
                layoutParams.y = ScreenUtils.getScreenHeight();
                layoutParams.height = oldMaxHeight;
                layoutParams.width = ScreenUtils.getScreenWidth();
                windowManager.updateViewLayout(LoggerView.this, layoutParams);
                mViewHolder.mLayoutMain.setVisibility(VISIBLE);
                mViewHolder.mIvMax.setVisibility(GONE);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    public void filterText(String filterWords) {
        mViewHolder.mTvFilter.setText(filterWords);
        mViewHolder.mLoggerView.setFilterText(filterWords);
    }


    private void initSelector() {
        mViewHolder.mLayoutSelector.removeAllViews();
        for (int i = 0; i < 5; i++) {
            TextView tv = new TextView(getContext());
            String levelName = "";
            if (i == 0)
                levelName = "Verbose";
            if (i == 1)
                levelName = "Debug";
            if (i == 2)
                levelName = "Info";
            if (i == 3)
                levelName = "Warn";
            if (i == 4)
                levelName = "Error";
            tv.setText(levelName);
            tv.setTag(i);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12);
            tv.setPadding(0, 4 * DP, 0, 4 * DP);
            tv.setTextColor(0xff0498FF);
            tv.setOnClickListener(new OnClickFilteListener() {
                @Override
                public void throttleClick(View view) {
                    mViewHolder.mTvSelector.setText(((TextView) view).getText());
                    mViewHolder.mLoggerView.setLevel((Integer) view.getTag());
                    mViewHolder.mLayoutSelector.setVisibility(GONE);
                }
            });
            mViewHolder.mLayoutSelector.addView(tv);
        }
    }

    public void onItemClick(LoggerMainView.ItemViewHolder itemViewHolder) {

    }

    public boolean onItemLongClick(final LoggerMainView.ItemViewHolder itemViewHolder) {
        mViewHolder.mLayoutMenuContent.removeAllViews();
        TextView tv = createMenuItem(getContext().getString(com.hive.i8n.R.string.base_copy_item));
        tv.setOnClickListener(new OnClickFilteListener() {
            @Override
            public void throttleClick(View view) {
                itemViewHolder.copyMsg();
                mViewHolder.mLayoutMenu.setVisibility(GONE);
            }
        });
        mViewHolder.mLayoutMenuContent.addView(tv);

        tv = createMenuItem(getContext().getString(com.hive.i8n.R.string.base_format));
        tv.setOnClickListener(new OnClickFilteListener() {
            @Override
            public void throttleClick(View view) {
                itemViewHolder.formatJson();
                mViewHolder.mLayoutMenu.setVisibility(GONE);
            }
        });
        mViewHolder.mLayoutMenuContent.addView(tv);

        tv = createMenuItem(getContext().getString(com.hive.i8n.R.string.base_collapse_item));
        tv.setOnClickListener(new OnClickFilteListener() {
            @Override
            public void throttleClick(View view) {
                itemViewHolder.closeMsg();
                mViewHolder.mLayoutMenu.setVisibility(GONE);
            }
        });
        mViewHolder.mLayoutMenuContent.addView(tv);

        mViewHolder.mLayoutMenu.setVisibility(VISIBLE);
        return true;
    }

    private TextView createMenuItem(String name) {
        TextView tv = new TextView(getContext());
        tv.setText(name);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        tv.setPadding(4 * DP, 4 * DP, 4 * DP, 4 * DP);
        tv.setTextColor(0xff0498FF);
        return tv;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.layout_menu) {
            mViewHolder.mLayoutMenu.setVisibility(GONE);
        } else if (v.getId() == R.id.iv_min) {
            minimumLoggerView();
        } else if (v.getId() == R.id.iv_close) {
            detachFromWindow();
            EngineerConfig config = EngineerConfig.read();
            config.loggerOn = false;
            config.save();
        } else if (v.getId() == R.id.iv_clear) {
            mViewHolder.mLoggerView.clear();
        } else if (v.getId() == R.id.iv_pause) {
            mViewHolder.mIvPause.setSelected(!mViewHolder.mIvPause.isSelected());
        } else if (v.getId() == R.id.tv_selector) {
            if (mViewHolder.mLayoutSelector.getVisibility() == VISIBLE) {
                mViewHolder.mLayoutSelector.setVisibility(GONE);
            } else {
                mViewHolder.mLayoutSelector.setVisibility(VISIBLE);
            }
        } else if (v.getId() == R.id.tv_filter) {
            DialogLoggerInput.start(getContext(), mViewHolder.mLoggerView.mFilterWords);
        }
    }

    private void minimumLoggerView() {
        layoutParams.x = oldMinX;
        layoutParams.y = oldMinY;
        layoutParams.width = 48 * DP;
        layoutParams.height = 48 * DP;
        windowManager.updateViewLayout(this, layoutParams);
        mViewHolder.mIvMin.setSelected(!mViewHolder.mIvMin.isSelected());
        mViewHolder.mLayoutMain.setVisibility(GONE);
        mViewHolder.mIvMax.setVisibility(VISIBLE);
    }


    public boolean onMinTouchEvent(MotionEvent event) {
        mGesture.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                requestDisallowInterceptTouchEvent(true);
                x = event.getRawX();
                y = event.getRawY();
                hasMove = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float nowX = event.getRawX();
                float nowY = event.getRawY();
                float movedX = nowX - x;
                float movedY = nowY - y;
                if (Math.sqrt(Math.pow(nowY - y, 2d) + Math.pow(nowX - x, 2d)) > 4 * DP) {
                    hasMove = true;
                }
                x = nowX;
                y = nowY;
                layoutParams.x = (int) (layoutParams.x + movedX);
                layoutParams.y = (int) (layoutParams.y + movedY);
                oldMinX = layoutParams.x;
                oldMinY = layoutParams.y;
                windowManager.updateViewLayout(this, layoutParams);
                break;
            case MotionEvent.ACTION_UP: {
                hasMove = false;
            }
            default:
                break;
        }
        return true;
    }

    private boolean hasMove = false;

    public boolean onTopTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float nowY = event.getRawY();
                y = nowY;
                layoutParams.y = ScreenUtils.getScreenHeight();
                layoutParams.height = (int) (ScreenUtils.getScreenHeight() - y);
                layoutParams.width = ScreenUtils.getScreenWidth();
                if (layoutParams.height < 200 * DP) {
                    layoutParams.height = 200 * DP;
                }
                oldMaxHeight = layoutParams.height;
                windowManager.updateViewLayout(this, layoutParams);
                break;
            default:
                break;
        }
        return true;
    }

    public boolean attachToWindow(Activity activity) {
        if (activity != null && !FloatPermissionAdapter.checkFloatPermission(activity)) {
            return false;
        }
        if (this.getParent() != null) return false;
        detachFromWindow();
        windowManager = (WindowManager) GlobalApp.getContext().getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        // 设置LayoutParam
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.width = MATCH_PARENT;
        layoutParams.height = oldMaxHeight;
//        layoutParams.alpha = 0;
        layoutParams.y = ScreenUtils.getScreenHeight();
        windowManager.addView(this, layoutParams);
        attachSystemLog();
        return true;
    }

    private void attachSystemLog() {
        DLog.registerProxy(this);
    }

    public static void detachFromWindow() {
        if (sInstance == null || sInstance.getParent() == null) return;
        if (sInstance.windowManager == null) return;
        sInstance.windowManager.removeViewImmediate(sInstance);
        DLog.unregisterProxy(sInstance);
    }

    @Override
    public void handleMessage(Message msg) {
        if (mViewHolder.mIvPause.isSelected()) return;
        LoggerMainView.DataBean bean = new LoggerMainView.DataBean();
        bean.type = msg.what;
        bean.tag = (String) ((Pair) msg.obj).first;
        bean.msg = (String) ((Pair) msg.obj).second;
        mViewHolder.mLoggerView.addMsg(bean);
    }

    @Override
    public void v(String tag, String header, String msg) {
        Message message = Message.obtain();
        message.what = 0;
        message.obj = new Pair<>(tag, header + "\n" + msg);
        mHandler.sendMessage(message);
    }

    @Override
    public void d(String tag, String header, String msg) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = new Pair<>(tag, header + "\n" + msg);
        mHandler.sendMessage(message);
    }

    @Override
    public void i(String tag, String header, String msg) {
        Message message = Message.obtain();
        message.what = 2;
        message.obj = new Pair<>(tag, header + "\n" + msg);
        mHandler.sendMessage(message);
    }

    @Override
    public void w(String tag, String header, String msg) {
        Message message = Message.obtain();
        message.what = 3;
        message.obj = new Pair<>(tag, header + "\n" + msg);
        mHandler.sendMessage(message);
    }

    @Override
    public void e(String tag, String header, String msg) {
        Message message = Message.obtain();
        message.what = 4;
        message.obj = new Pair<>(tag, header + "\n" + msg);
        mHandler.sendMessage(message);
    }

    @Override
    public int getLayoutId() {
        return R.layout.logger_view;
    }

}
