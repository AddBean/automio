// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.hive.adapter.RecyclerListAdapter;
import com.hive.base.R;
import com.hive.utils.GlobalApp;

public class FooterViewHolder extends RecyclerView.ViewHolder {

    private TextView mTextMsg;
    private View mViewAnim;
    private View mItemView;

    public FooterViewHolder(View view) {
        super(view);
        mItemView = view;
        if (mItemView == null) return;
        mTextMsg = mItemView.findViewById(R.id.text_msg);
        mViewAnim = mItemView.findViewById(R.id.view_anim);
    }

    public void setState(RecyclerListAdapter.ELoadState mLoadState) {
        if (mItemView == null) return;
        if(mTextMsg ==null|| mViewAnim ==null)return;
        switch (mLoadState) {
            case GONE:
                mItemView.setVisibility(View.GONE);
                break;
            case FAILED:
                mTextMsg.setText(GlobalApp.getString(com.hive.i8n.R.string.base_list_loading_failed));
                mViewAnim.setVisibility(View.INVISIBLE);
                mItemView.setVisibility(View.VISIBLE);
                break;
            case LOADING:
                mTextMsg.setText(GlobalApp.getString(com.hive.i8n.R.string.base_list_loading));
                mViewAnim.setVisibility(View.VISIBLE);
                mItemView.setVisibility(View.VISIBLE);
                break;
            case READY:
                mTextMsg.setText(GlobalApp.getString(com.hive.i8n.R.string.base_list_uppull));
                mViewAnim.setVisibility(View.INVISIBLE);
                mItemView.setVisibility(View.VISIBLE);
                break;
            case EMPTY:
                mTextMsg.setText(GlobalApp.getString(com.hive.i8n.R.string.base_list_empty));
                mViewAnim.setVisibility(View.INVISIBLE);
                mItemView.setVisibility(View.VISIBLE);
                break;
        }
    }
}
