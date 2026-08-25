// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.adapter.core;

public class CardItemData {
    public int cardType = 0;
    public int position;
    public boolean selected = false;
    public boolean editModel = false;
    public int dividerState;
    public Object data;
    public Object ext;

    public CardItemData(int cardType, Object data) {
        this.cardType = cardType;
        this.data = data;
    }

    public CardItemData(int cardType,Object data, Object ext) {
        this.cardType = cardType;
        this.ext = ext;
        this.data = data;
    }


    public CardItemData(int cardType,Object data, boolean editModel) {
        this.cardType = cardType;
        this.editModel = editModel;
        this.data = data;
    }


    public CardItemData(int cardType) {
        this.cardType = cardType;
    }

    public CardItemData(Object data) {
        this.data = data;
    }

    public CardItemData() {
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setCardType( int cardType) {
        this.cardType = cardType;
    }

    public int getCardType() {
        return cardType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getDividerState() {
        return dividerState;
    }

    public void setDividerState(int dividerState) {
        this.dividerState = dividerState;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEditModel() {
        return editModel;
    }

    public void setEditModel(boolean editModel) {
        this.editModel = editModel;
    }

    public Object getExt() {
        return ext;
    }

    public void setExt(Object ext) {
        this.ext = ext;
    }
}
