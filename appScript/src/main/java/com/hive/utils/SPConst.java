// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

public class SPConst {
    public static String USER_INVITE_CODE = "user_invite_code";
    public static String FIRST_IN_MARK = "first_in_mark";
    public static String USER_LOCAL_MESSAGE_COUNT = "user_local_message_count";

    public static String getAgreementUrl() {
        if (GlobalApp.getFlavorName().equals("google")) {
            return "https://docs.google.com/document/d/e/2PACX-1vTDZ1WDQJaJ7w7zSWLIrs86qVazcqznKRXl5MFpG-ykO5tGwgSTiYCU_OA5653-99FCVXSNZUnNlzMr/pub";
        } else {
            return "http://static.nichanai.cn/scipt_agreement.html";
        }
    }

    public static String getPrivacyUrl() {
        if (GlobalApp.getFlavorName().equals("google")) {
            return "https://docs.google.com/document/u/0/d/e/2PACX-1vS3Dw-rBDjlwIkDF-nHqhd0_D3Z9DSNnLzH32WzeDt2hORdJtjPLlVtS4_ckbRkXqXvyCvcp9UfoT_E/pub";
        } else {
            return "http://static.nichanai.cn/scipt_privacy.html";
        }
    }
}
