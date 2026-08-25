// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.resp;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hive.annotation.NotProguard;
@NotProguard
public class DialogModel {

    /**
     * dialogContent : 这是测试链接<br/>http://www.baidu.com
     * dialogTitle : 这是测试标题
     * dialogOpen : 1
     * dialogTimes : 5
     * dialogType : 1
     * btnUrl : http://www.baidu.com
     * btnText : <font color=\"#FF0000\">点击跳转</font>
     * btnType : 1
     */
    @SerializedName("dialogContent")
    @Expose
    private String dialogContent;
    @SerializedName("dialogTitle")
    @Expose
    private String dialogTitle;
    @SerializedName("dialogOpen")
    @Expose
    private int dialogOpen;
    @SerializedName("dialogType")
    @Expose
    private int dialogType;

    @SerializedName("showType")
    @Expose
    private int showType;
    @SerializedName("btnUrl")
    @Expose
    private String btnUrl;
    @SerializedName("btnText")
    @Expose
    private String btnText;
    @SerializedName("cancelText")
    @Expose
    private String cancelText;

    public int getShowType() {
        return showType;
    }

    public void setShowType(int showType) {
        this.showType = showType;
    }

    public String getDialogContent() {
        return dialogContent;
    }

    public void setDialogContent(String dialogContent) {
        this.dialogContent = dialogContent;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public int getDialogOpen() {
        return dialogOpen;
    }

    public void setDialogOpen(int dialogOpen) {
        this.dialogOpen = dialogOpen;
    }


    public int getDialogType() {
        return dialogType;
    }

    public void setDialogType(int dialogType) {
        this.dialogType = dialogType;
    }

    public String getBtnUrl() {
        return btnUrl;
    }

    public void setBtnUrl(String btnUrl) {
        this.btnUrl = btnUrl;
    }

    public String getBtnText() {
        return btnText;
    }

    public void setBtnText(String btnText) {
        this.btnText = btnText;
    }

    public String getCancelText() {
        return cancelText;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }
}
