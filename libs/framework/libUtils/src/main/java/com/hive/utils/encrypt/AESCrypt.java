// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.encrypt;

import android.util.Base64;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AESCrypt {
    private static final String TAG = "AESCrypt";
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final String CHARSET = "UTF-8";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final byte[] ivBytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static boolean DEBUG_LOG_ENABLED = false;

    private static SecretKeySpec generateKey(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();
        log("SHA-256 key ", key);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    public static String encrypt(String password, String message) throws GeneralSecurityException {
        try {
            SecretKeySpec key = generateKey(password);
            log("message", message);
            byte[] cipherText = encrypt(key, ivBytes, message.getBytes("UTF-8"));
            String encoded = Base64.encodeToString(cipherText, 2);
            log("Base64.NO_WRAP", encoded);
            return encoded;
        } catch (UnsupportedEncodingException var5) {
            if (DEBUG_LOG_ENABLED) {
                Log.e("AESCrypt", "UnsupportedEncodingException ", var5);
            }

            throw new GeneralSecurityException(var5);
        }
    }

    public static byte[] encrypt(SecretKeySpec key, byte[] iv, byte[] message) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(1, key, ivSpec);
        byte[] cipherText = cipher.doFinal(message);
        log("cipherText", cipherText);
        return cipherText;
    }

    public static String decrypt(String password, String base64EncodedCipherText) throws GeneralSecurityException {
        try {
            SecretKeySpec key = generateKey(password);
            log("base64EncodedCipherText", base64EncodedCipherText);
            byte[] decodedCipherText = Base64.decode(base64EncodedCipherText, 2);
            log("decodedCipherText", decodedCipherText);
            byte[] decryptedBytes = decrypt(key, ivBytes, decodedCipherText);
            log("decryptedBytes", decryptedBytes);
            String message = new String(decryptedBytes, "UTF-8");
            log("message", message);
            return message;
        } catch (UnsupportedEncodingException var6) {
            if (DEBUG_LOG_ENABLED) {
                Log.e("AESCrypt", "UnsupportedEncodingException ", var6);
            }

            throw new GeneralSecurityException(var6);
        }
    }

    public static byte[] decrypt(SecretKeySpec key, byte[] iv, byte[] decodedCipherText) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(2, key, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(decodedCipherText);
        log("decryptedBytes", decryptedBytes);
        return decryptedBytes;
    }

    private static void log(String what, byte[] bytes) {
        if (DEBUG_LOG_ENABLED) {
            Log.d("AESCrypt", what + "[" + bytes.length + "] [" + bytesToHex(bytes) + "]");
        }

    }

    private static void log(String what, String value) {
        if (DEBUG_LOG_ENABLED) {
            Log.d("AESCrypt", what + "[" + value.length() + "] [" + value + "]");
        }

    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];

        for(int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 15];
        }

        return new String(hexChars);
    }

    private AESCrypt() {
    }
}
