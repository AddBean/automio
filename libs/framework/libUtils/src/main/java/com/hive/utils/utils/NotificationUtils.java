// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public final class NotificationUtils {
    private static int titleColor;
    private final static String TAG = NotificationUtils.class.getSimpleName();
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    private static final double COLOR_THRESHOLD = 180.0;

    public static final String Notification_Push_Category = "notification_push_category";

    public static final String Notification_TIPS_Category = "notification_tips_category";

    public static NotificationCompat.Builder createNotificationBuilder(@NonNull Context context, @NonNull String category) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //获取状态通知栏管理
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (null != manager && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = NotificationUtils.createNotificationChannelId(context, category);
                if (manager.getNotificationChannel(channelId) == null) {
                    int importance = NotificationManager.IMPORTANCE_LOW;
                    if (TextUtils.equals(category, NotificationUtils.Notification_Push_Category)) {
                        importance = NotificationManager.IMPORTANCE_HIGH;
                    }
                    NotificationChannel channel = new NotificationChannel(channelId,
                            NotificationUtils.createNotificationChannelName(context, category),
                            importance);

                    manager.createNotificationChannel(channel);
                }
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.createNotificationChannelId(context, category));
        return builder;
    }

    public static String createNotificationChannelName(Context context, @NonNull String category) {
        if (TextUtils.equals(category, Notification_Push_Category)) {
            return context.getString(com.hive.i8n.R.string.utils_notification_channel_push);
        } else {
            return context.getString(com.hive.i8n.R.string.utils_notification_channel_tips);
        }
    }

    public static String createNotificationChannelId(Context context, @NonNull String category) {
        if (TextUtils.equals(category, Notification_Push_Category)) {
            return "notification_channel_push";
        } else {
            return "notification_channel_tips";
        }
    }


    private static void buildNotificationChannel(@NonNull Context context, @NonNull String category, NotificationManager manager) {
        if (null != manager && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = createNotificationChannelId(context, category);

            if (manager.getNotificationChannel(channelId) == null) {

                int importance = NotificationManager.IMPORTANCE_LOW;

                if (/*TextUtils.equals(category, Notification_WifiCalendar_Category)
                        || */TextUtils.equals(category, Notification_Push_Category)
                        ) {
                    importance = NotificationManager.IMPORTANCE_HIGH;
                }

                NotificationChannel channel = new NotificationChannel(channelId,
                        createNotificationChannelName(context, category),
                        importance);

//                有手机会崩溃
//                channel.setGroup(createNotificationGroupId(context, category));
//                try {
                manager.createNotificationChannel(channel);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }
    }


    public static void beforeCreateNotificationBuilder(@NonNull Context context, @NonNull String category, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildNotificationChannel(context, category, manager);
        }
    }



    /**
     * 判断通知栏背景颜色，现在手机通知栏大部分不是白色就是黑色背景
     *
     * @param context
     * @return
     */
    public static boolean isDarkNotiFicationBar(Context context) {
        return !isColorSimilar(Color.BLACK, getNotificationColor(context));
    }

    private static int getNotificationColor(Context context) {
        if (context instanceof AppCompatActivity) {
            return getNotificationColorCompat(context);
        } else {
            return getNotificationColorInternal(context);
        }
    }

    private static boolean isColorSimilar(int baseColor, int color) {
        int simpleBaseColor = baseColor | 0xff000000;
        int simpleColor = color | 0xff000000;
        int baseRed = Color.red(simpleBaseColor) - Color.red(simpleColor);
        int baseGreen = Color.green(simpleBaseColor) - Color.green(simpleColor);
        int baseBlue = Color.blue(simpleBaseColor) - Color.blue(simpleColor);
        double value = Math.sqrt(baseRed * baseRed + baseGreen * baseGreen + baseBlue * baseBlue);
        if (value < COLOR_THRESHOLD) {
            return true;
        }
        return false;
    }

    private static int getNotificationColorInternal(Context context) {
        final String DUMMY_TITLE = "DUMMY_TITLE";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentText(DUMMY_TITLE);
        Notification notification = builder.build();
        RemoteViews contentView = notification.contentView;
        if (contentView == null) {
            return 0;
        }
        ViewGroup notificationRoot = (ViewGroup) contentView.apply(context, new FrameLayout(context));
        final TextView titleView = (TextView) notificationRoot.findViewById(android.R.id.title);
        if (titleView == null) {
            iteratoryView(notificationRoot, new Filter() {
                @Override
                public void filter(View view) {
                    if (view instanceof TextView) {
                        TextView textView = (TextView) view;
                        if (DUMMY_TITLE.equals(textView.getText().toString())) {
                            titleColor = textView.getCurrentTextColor();
                        }
                    }
                }
            });
            return titleColor;
        } else {
            return titleView.getCurrentTextColor();
        }
    }

    private static int getNotificationColorCompat(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Notification notification = builder.build();
        RemoteViews contentView = notification.contentView;
        if ( contentView == null ) {
            return 0;
        }
        int layoutId = contentView.getLayoutId();
        ViewGroup notificationRoot = (ViewGroup) LayoutInflater.from(context).inflate(layoutId, null);
        final TextView titleView = (TextView) notificationRoot.findViewById(android.R.id.title);
        if (titleView == null) {
            final List<TextView> textViews = new ArrayList<>();
            iteratoryView(notificationRoot, new Filter() {
                @Override
                public void filter(View view) {
                    textViews.add((TextView) view);
                }
            });
            float minTextSize = Integer.MIN_VALUE;
            int index = 0;
            for (int i = 0, j = textViews.size(); i < j; i++) {
                float currentSize = textViews.get(i).getTextSize();
                if (currentSize > minTextSize) {
                    minTextSize = currentSize;
                    index = i;
                }
            }
            return textViews.get(index).getCurrentTextColor();
        } else {
            return titleView.getCurrentTextColor();
        }
    }
    private static void iteratoryView(View view, Filter filter) {
        if (view == null || filter == null) {
            return;
        }
        filter.filter(view);
        if (view instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) view;
            for (int i = 0, j = container.getChildCount(); i < j; i++) {
                View child = container.getChildAt(i);
                iteratoryView(child, filter);
            }
        }
    }
    private interface Filter {
        void filter(View view);
    }
}
