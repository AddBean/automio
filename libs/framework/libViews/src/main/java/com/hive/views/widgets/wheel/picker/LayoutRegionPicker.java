// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.wheel.picker;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.hive.views.R;
import com.hive.views.widgets.wheel.OnWheelChangedListener;
import com.hive.views.widgets.wheel.WheelView;
import com.hive.views.widgets.wheel.adapters.ArrayWheelAdapter;
import com.hive.views.widgets.wheel.model.CityModel;
import com.hive.views.widgets.wheel.model.DistrictModel;
import com.hive.views.widgets.wheel.model.ProvinceModel;
import com.hive.views.widgets.wheel.service.XmlParserHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by AddBean.
 */

public class LayoutRegionPicker extends FrameLayout implements OnWheelChangedListener {
    private WheelView mViewProvince;
    private WheelView mViewCity;
    public WheelView mViewDistrict;
    private View mView;

    public LayoutRegionPicker(Context context) {
        super(context);
        initView();
    }

    public LayoutRegionPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public LayoutRegionPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.layout_wheel_picker, this);
        setUpViews();
        setUpListener();
        setUpData();
    }


    private void setUpViews() {
        mViewProvince = (WheelView) findViewById(R.id.wheel_1);
        mViewCity = (WheelView) findViewById(R.id.wheel_2);
        mViewDistrict = (WheelView) findViewById(R.id.wheel_3);
    }

    private void setUpListener() {
        // 添加change事件,省
        mViewProvince.addChangingListener(this);
        // 添加change事件，市
        mViewCity.addChangingListener(this);
        // 添加change事件，区
        mViewDistrict.addChangingListener(this);
    }

    private void setUpData() {
        initProvinceDatas();
        mViewProvince.setViewAdapter(new ArrayWheelAdapter<String>(
                getContext(), mProvinceDatas));
        // 设置可见条目数量
        mViewProvince.setVisibleItems(7);
        mViewCity.setVisibleItems(7);
        mViewDistrict.setVisibleItems(7);
        updateCities();
        updateAreas();
    }

    @Override
    public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if (wheel == mViewProvince) {
            updateCities();
        } else if (wheel == mViewCity) {
            updateAreas();
        } else if (wheel == mViewDistrict) {
            updateZipCode();
        }
        if (mOnRegionPickerListener != null)
            mOnRegionPickerListener.onPicked(mCurrentProviceName, mCurrentCityName, mCurrentDistrictName, mCurrentZipCode);
    }

    /**
     * 根据当前的省，更新市WheelView的信息
     */
    private void updateCities() {
        try {
            int pCurrent = mViewProvince.getCurrentItem();
            // 将当前的省赋值给全局
            mCurrentProviceName = mProvinceDatas[pCurrent];
            String[] cities = mCitisDatasMap.get(mCurrentProviceName);
            if (cities == null) {
                cities = new String[]{""};
            }
            mViewCity.setViewAdapter(new ArrayWheelAdapter<String>(getContext(), cities));
            mViewCity.setCurrentItem(0);
            updateAreas();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 根据当前的市，更新区WheelView的信息
     */
    private void updateAreas() {
        try {
            int pCurrent = mViewCity.getCurrentItem();
            // 将当前的市赋值给全局
            mCurrentCityName = mCitisDatasMap.get(mCurrentProviceName)[pCurrent];
            String[] areas = mDistrictDatasMap.get(mCurrentCityName);

            if (areas == null) {
                areas = new String[]{""};
            }
            mViewDistrict.setViewAdapter(new ArrayWheelAdapter<String>(getContext(), areas));
            mViewDistrict.setCurrentItem(0);
            updateZipCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将变化的区和邮政编码更新到全局
     */
    private void updateZipCode() {
        try {
            int pCurrent = mViewDistrict.getCurrentItem();
            mCurrentDistrictName = mDistrictDatasMap.get(mCurrentCityName)[pCurrent];
            mCurrentZipCode = mZipcodeDatasMap.get(mCurrentDistrictName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 所有省
     */
    protected String[] mProvinceDatas;
    /**
     * key - 省 value - 市
     */
    protected Map<String, String[]> mCitisDatasMap = new HashMap<String, String[]>();
    /**
     * key - 市 values - 区
     */
    protected Map<String, String[]> mDistrictDatasMap = new HashMap<String, String[]>();

    /**
     * key - 区 values - 邮编
     */
    protected Map<String, String> mZipcodeDatasMap = new HashMap<String, String>();

    /**
     * 当前省的名称
     */
    protected String mCurrentProviceName;
    /**
     * 当前市的名称
     */
    protected String mCurrentCityName;
    /**
     * 当前区的名称
     */
    protected String mCurrentDistrictName = "";

    /**
     * 当前区的邮政编码
     */
    protected String mCurrentZipCode = "";

    /**
     * 解析省市区的XML数据
     */

    protected void initProvinceDatas() {
        List<ProvinceModel> provinceList = null;
        AssetManager asset = getContext().getAssets();
        try {
            InputStream input = asset.open("province_data.xml");
            // 创建一个解析xml的工厂对象
            SAXParserFactory spf = SAXParserFactory.newInstance();
            // 解析xml
            SAXParser parser = spf.newSAXParser();
            XmlParserHandler handler = new XmlParserHandler();
            parser.parse(input, handler);
            input.close();
            // 获取解析出来的数据
            provinceList = handler.getDataList();
            //*/ 初始化默认选中的省、市、区
            if (provinceList != null && !provinceList.isEmpty()) {
                mCurrentProviceName = provinceList.get(0).getName();
                List<CityModel> cityList = provinceList.get(0).getCityList();
                if (cityList != null && !cityList.isEmpty()) {
                    mCurrentCityName = cityList.get(0).getName();
                    List<DistrictModel> districtList = cityList.get(0).getDistrictList();
                    mCurrentDistrictName = districtList.get(0).getName();
                    mCurrentZipCode = districtList.get(0).getZipcode();
                }
            }
            //*/
            mProvinceDatas = new String[provinceList.size()];
            for (int i = 0; i < provinceList.size(); i++) {
                // 遍历所有省的数据
                mProvinceDatas[i] = provinceList.get(i).getName();
                List<CityModel> cityList = provinceList.get(i).getCityList();
                String[] cityNames = new String[cityList.size()];
                for (int j = 0; j < cityList.size(); j++) {
                    // 遍历省下面的所有市的数据
                    cityNames[j] = cityList.get(j).getName();
                    List<DistrictModel> districtList = cityList.get(j).getDistrictList();
                    String[] distrinctNameArray = new String[districtList.size()];
                    DistrictModel[] distrinctArray = new DistrictModel[districtList.size()];
                    for (int k = 0; k < districtList.size(); k++) {
                        // 遍历市下面所有区/县的数据
                        DistrictModel districtModel = new DistrictModel(districtList.get(k).getName(), districtList.get(k).getZipcode());
                        // 区/县对于的邮编，保存到mZipcodeDatasMap
                        mZipcodeDatasMap.put(districtList.get(k).getName(), districtList.get(k).getZipcode());
                        distrinctArray[k] = districtModel;
                        distrinctNameArray[k] = districtModel.getName();
                    }
                    // 市-区/县的数据，保存到mDistrictDatasMap
                    mDistrictDatasMap.put(cityNames[j], distrinctNameArray);
                }
                // 省-市的数据，保存到mCitisDatasMap
                mCitisDatasMap.put(provinceList.get(i).getName(), cityNames);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {

        }
    }

    private OnRegionPickerListener mOnRegionPickerListener;

    public void setmOnRegionPickerListener(OnRegionPickerListener mOnTimePickerListener) {
        this.mOnRegionPickerListener = mOnTimePickerListener;
    }

    public interface OnRegionPickerListener {
        void onPicked(String mCurrentProviceName, String mCurrentCityName, String mCurrentDistrictName, String mCurrentZipCode);
    }
}
