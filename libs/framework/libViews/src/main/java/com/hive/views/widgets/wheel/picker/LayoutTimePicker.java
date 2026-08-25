// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.wheel.picker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;


import com.hive.views.R;
import com.hive.views.widgets.wheel.OnWheelChangedListener;
import com.hive.views.widgets.wheel.WheelView;
import com.hive.views.widgets.wheel.adapters.ArrayWheelAdapter;
import com.hive.views.widgets.wheel.adapters.ListWheelAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by AddBean.
 */

public class LayoutTimePicker extends FrameLayout implements OnWheelChangedListener {
    private WheelView mViewProvince;
    private WheelView mViewCity;
    private WheelView mViewDistrict;
    private View mView;
    protected List<String> mDayList;
    protected List<String> mYearList = new ArrayList<String>();
    protected String[] mMonthList = new String[]{"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
    String mCurYear;// 每次更新时存储到这个里面
    String mCurMonth;
    String mCurDay;
    String mYearTemp;// 这个只记录第一次获取的时间
    String mMonthTemp;
    String mDayTemp;


    public LayoutTimePicker(Context context) {
        super(context);
        initView();
    }

    public LayoutTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public LayoutTimePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.layout_wheel_picker, this);
        Calendar calendar = Calendar.getInstance();
        mCurYear = mYearTemp = calendar.get(Calendar.YEAR) + "";// 获取当前年
        mCurMonth = mMonthTemp = calendar.get(Calendar.MONTH) + 1 + "";// 获取当前月
        mCurDay = mDayTemp = calendar.get(Calendar.DAY_OF_MONTH) + "";// 获取当前日期
        setYear(1930, Integer.parseInt(mCurYear));// 获取年份列表，上一百年到下一百年
        setupViews();// 实例化控件
        setupListener();// 为控件添加监听
        setupData();// 设置
        setTime();
    }

    /**
     * 将当前的时间添加进时间选择框
     */
    private void setTime() {
        int yy = 0;
        int mm = 0;
        int dd = 0;
        for (int i = 0; i < mYearList.size(); i++) {
            if (mYearList.get(i).equals(mYearTemp + "年")) {
                yy = i;
                System.out.println("yy  " + yy);
            }
        }
        for (int i = 0; i < mMonthList.length; i++) {
            if (mMonthList[i].equals(mMonthTemp + "月")) {
                mm = i;
                System.out.println("mm  " + mm);
            }
        }
        for (int i = 0; i < mDayList.size(); i++) {
            if (mDayList.get(i).equals(mDayTemp + "日")) {
                dd = i;
                System.out.println("dd  " + dd);
            }
        }
        mViewProvince.setCurrentItem(yy);
        mViewCity.setCurrentItem(mm);
        mViewDistrict.setCurrentItem(dd);
    }

    private void setupViews() {
        mViewProvince = (WheelView) mView.findViewById(R.id.wheel_1);
        mViewCity = (WheelView) mView.findViewById(R.id.wheel_2);
        mViewDistrict = (WheelView) mView.findViewById(R.id.wheel_3);
        mViewProvince.setCyclic(false);// 设置循环
        mViewCity.setCyclic(false);
        mViewDistrict.setCyclic(false);
    }

    private void setupListener() {
        // 添加change事件,年
        mViewProvince.addChangingListener(this);
        // 添加change事件，月
        mViewCity.addChangingListener(this);
        // 添加change事件，日
        mViewDistrict.addChangingListener(this);
    }

    private void setupData() {
        // 设置可见条目数量
        mViewProvince.setVisibleItems(7);
        mViewCity.setVisibleItems(7);
        mViewDistrict.setVisibleItems(7);
        updateYear();
        updateMonth();
    }

    @Override
    public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if (wheel == mViewProvince) {// 滑动年滚轮的事件
            updateYear();
        } else if (wheel == mViewCity) {// 滑动月滚轮的事件
            updateMonth();
        } else if (wheel == mViewDistrict) {// 滑动日滚轮的事件
            updateDay();
        }
        if (mOnTimePickerListener != null)
            mOnTimePickerListener.onPicked(mCurYear.replace("年",""), mCurMonth.replace("月",""), mCurDay.replace("日",""));
    }

    private void updateYear() {// 将年份的数据添加进滚轮中
        mViewProvince.setViewAdapter(new ListWheelAdapter<String>(
                getContext(), mYearList));
        try {
            int pCurrent = mViewProvince.getCurrentItem();
            // 将当前的年赋值给全局
            mCurYear = mYearList.get(pCurrent);
            // 因为在第一次进入的时候是还没有月份的数据的，所以需要排除掉
            if (mCurMonth != null) {
                getDay(Integer.parseInt(mCurYear.replace("年", "")), Integer.parseInt(mCurMonth.replace("月", "")));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMonth() {
        mViewCity.setViewAdapter(new ArrayWheelAdapter<String>(
                getContext(), mMonthList));
        try {
            int pCurrent = mViewCity.getCurrentItem();
            // 将当前的月赋值给全局
            mCurMonth = mMonthList[pCurrent];
            getDay(Integer.parseInt(mCurYear.replace("年", "")),
                    Integer.parseInt(mCurMonth.replace("月", "")));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDay() {
        try {
            int pCurrent = mViewDistrict.getCurrentItem();
            mCurDay = mDayList.get(pCurrent);
        } catch (Exception e) {
            mViewDistrict.setCurrentItem(mDayList.size() - 1);
            mCurDay = mDayList.get(mDayList.size() - 1);
        }
    }


    /**
     * 获取上下两百年的年份信息
     */
    private void setYear(int start, int end) {
        for (int i = start; i <= end; i++) {
            mYearList.add(i + "" + "年");
        }
    }

    /**
     * 根据当前的年月来获取日的信息
     *
     * @param year
     * @param month
     */
    private void getDay(int year, int month) {
        mDayList = new ArrayList<String>();
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month);
        a.set(Calendar.DATE, 1);
        a.set(Calendar.DATE, -1);
        int maxdate = a.get(Calendar.DATE);
        for (int i = 1; i <= maxdate + 1; i++) {
            mDayList.add(i + "日");
        }
        mViewDistrict.setViewAdapter(new ListWheelAdapter<String>(
                getContext(), mDayList));
        updateDay();
    }

    private OnTimePickerListener mOnTimePickerListener;

    public void setmOnTimePickerListener(OnTimePickerListener mOnTimePickerListener) {
        this.mOnTimePickerListener = mOnTimePickerListener;
    }

    public interface OnTimePickerListener {
        void onPicked(String year, String month, String day);
    }
}
