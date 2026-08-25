// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hive.editor.R;
import com.hive.richeditor.core.BaseLayout;
import com.hive.richeditor.core.RichEditor;

/**
 * Created by Administrator on 2017/7/4.
 */

public class EditMenuAttachment extends BaseLayout {

    private static EditMenuAttachment sIntsance;
    private ViewHolder mViewHolder;
    private Activity mActivity;

    static class ViewHolder {
        ImageButton mActionImage;
        ImageButton mActionLink;
        ImageButton mActionSplit;
        LinearLayout mLlLayoutAdd;

        ViewHolder(View view) {
            mLlLayoutAdd = (LinearLayout) view.findViewById(R.id.ll_layout_add);
            mActionImage = (ImageButton) view.findViewById(R.id.action_image);
            mActionLink = (ImageButton) view.findViewById(R.id.action_link);
            mActionSplit = (ImageButton) view.findViewById(R.id.action_split);
        }
    }

    private RichEditor mEditor;

    public static EditMenuAttachment getInstance(Context context) {
        if (sIntsance == null)
            sIntsance = new EditMenuAttachment(context);
        return sIntsance;
    }

    public EditMenuAttachment(Context context) {
        super(context);
    }

    public void attachEditor(Activity activity, RichEditor richEditor) {
        mActivity = activity;
        mEditor = richEditor;
    }

    @Override
    protected void initView(View view) {
        mViewHolder = new ViewHolder(view);
        bindAttachEvent();
    }

    /**
     * 图片、链接等操作；
     */
    private void bindAttachEvent() {
        mViewHolder.mActionImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.dismiss();
                Intent picture = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                mActivity.startActivityForResult(picture, EditFragment.RICH_IMAGE_CODE);
            }
        });
        mViewHolder.mActionLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.dismiss();
                showInsertLinkDialog();
            }
        });
        mViewHolder.mActionSplit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuHelper.dismiss();
                mEditor.insertHr();
            }
        });

    }

    /**
     * 插入链接Dialog
     */
    private void showInsertLinkDialog() {

        AlertDialog.Builder adb = new AlertDialog.Builder(mActivity);
        final AlertDialog linkDialog = adb.create();
        View view = mActivity.getLayoutInflater().inflate(R.layout.dialog_insertlink, null);

        final EditText et_link_address = (EditText) view.findViewById(R.id.dialog_link_address);
        final EditText et_link_title = (EditText) view.findViewById(R.id.dialog_link_title);
        //点击确实的监听
        view.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String linkAddress = et_link_address.getText().toString();
                String linkTitle = et_link_title.getText().toString();
                if (linkAddress.endsWith("http://") || TextUtils.isEmpty(linkAddress)) {
                    Toast.makeText(getContext(), getContext().getString(com.hive.i8n.R.string.editor_link_address_required), Toast.LENGTH_SHORT);
                } else if (TextUtils.isEmpty(linkTitle)) {
                    Toast.makeText(getContext(), getContext().getString(com.hive.i8n.R.string.editor_link_title_required), Toast.LENGTH_SHORT);
                } else {
                    mEditor.insertLink(linkAddress, linkTitle);
                    linkDialog.dismiss();
                }
            }
        });
        //点击取消的监听
        view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linkDialog.dismiss();
            }
        });
        linkDialog.setView(view, 0, 0, 0, 0); // 设置 view
        linkDialog.show();
    }

    @Override
    public int getLayoutId() {
        return R.layout.edit_menu_attachment;
    }


}
