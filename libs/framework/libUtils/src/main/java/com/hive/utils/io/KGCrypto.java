// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.io;

import android.util.Base64;

import java.nio.charset.Charset;

/**
 * Created by cuishan on 11/4/16.
 */

public class KGCrypto {
    /*
     * RC4 Encode
     */
    public static String encodeString(String inputString, String key) {
        try {
            int[] iS = new int[256];
            byte[] iK = new byte[256];

            for (int i = 0; i < 256; i++)
                iS[i] = i;

            for (short i = 0; i < 256; i++) {
                iK[i] = (byte) key.charAt((i % key.length()));
            }

            int j = 0;
            for (int i = 0; i < 256; i++) {
                j = (j + iS[i] + iK[i]) % 256;
                int temp = iS[i];
                iS[i] = iS[j];
                iS[j] = temp;
            }

            int i = 0;
            j = 0;
            byte[] byteArray = inputString.getBytes(Charset.forName("UTF-8"));
            byte[] outArray = new byte[byteArray.length];
            for (int x = 0; x < byteArray.length; x++) {
                i = (i + 1) % 256;
                j = (j + iS[i]) % 256;
                int temp = iS[i];
                iS[i] = iS[j];
                iS[j] = temp;
                int t = (iS[i] + (iS[j] % 256)) % 256;
                int iY = iS[t];
                char iCY = (char) iY;
                outArray[x] = (byte) (byteArray[x] ^ iCY);
            }
            return new String(Base64.encode(outArray, Base64.NO_WRAP));
        } catch (Exception e) {
            e.printStackTrace();
            return "RmFpbGVkIQ=="; /* Failed! */
        }
    }

    /*
     * RC4 Decode
     */
    public static String decodeString(String inputString, String key) {
        try {
            byte[] inputArray = Base64.decode(inputString, Base64.NO_WRAP);

            int[] iS = new int[256];
            byte[] iK = new byte[256];

            for (int i = 0; i < 256; i++)
                iS[i] = i;

            for (short i = 0; i < 256; i++) {
                iK[i] = (byte) key.charAt((i % key.length()));
            }

            int j = 0;
            for (int i = 0; i < 256; i++) {
                j = (j + iS[i] + iK[i]) % 256;
                int temp = iS[i];
                iS[i] = iS[j];
                iS[j] = temp;
            }

            int i = 0;
            j = 0;
            byte[] outArray = new byte[inputArray.length];
            for (int x = 0; x < inputArray.length; x++) {
                i = (i + 1) % 256;
                j = (j + iS[i]) % 256;
                int temp = iS[i];
                iS[i] = iS[j];
                iS[j] = temp;
                int t = (iS[i] + (iS[j] % 256)) % 256;
                int iY = iS[t];
                char iCY = (char) iY;
                outArray[x] = (byte) (inputArray[x] ^ iCY);
            }
            return new String(outArray);
        } catch (Exception e) {
            return "Failed!";
        }
    }
}

