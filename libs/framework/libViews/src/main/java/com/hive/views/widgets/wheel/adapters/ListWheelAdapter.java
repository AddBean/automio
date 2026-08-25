/*
 *  Copyright 2011 Yuri Kanivets
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.hive.views.widgets.wheel.adapters;

import android.content.Context;

import java.util.List;

/**
 * 这是我自定义的list类型的，ArrayWheelAdapter的是String［］的
 * @author liu
 *
 * @param <T>
 */
public class ListWheelAdapter<T> extends AbstractWheelTextAdapter {
    
    // items
    private List<T> list;

    /**
     * Constructor
     * @param context the current context
     * @param items the items
     */
    public ListWheelAdapter(Context context, List<T> list) {
        super(context);
        
        //setEmptyItemResource(TEXT_VIEW_ITEM_RESOURCE);
        this.list = list;
    }
    
    @Override
    public CharSequence getItemText(int index) {
        if (index >= 0 && index < list.size()) {
            T item = list.get(index);
            if (item instanceof CharSequence) {
                return (CharSequence) item;
            }
            return item.toString();
        }
        return null;
    }

    @Override
    public int getItemsCount() {
        return list.size();
    }
}
