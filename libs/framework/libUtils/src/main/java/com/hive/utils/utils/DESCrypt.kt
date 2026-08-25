// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

class DESCrypt {
    //创建cipher对象
    val cipherEncypt = Cipher.getInstance("DES")

    //创建cipher对象
    val cipherDecypt = Cipher.getInstance("DES")

    //初始化cipher(参数：加密/解密模式)
    val kfEncypt = SecretKeyFactory.getInstance("DES")

    var keySpecEncypt: DESKeySpec? = null

    //初始化cipher(参数：加密/解密模式)
    val kfDecypt = SecretKeyFactory.getInstance("DES")

    var keySpecDecypt: DESKeySpec? = null

    fun init(password: String) {
        keySpecEncypt = DESKeySpec(password.toByteArray())
        var key: Key = kfEncypt.generateSecret(keySpecEncypt)
        cipherEncypt.init(Cipher.ENCRYPT_MODE, key)
        keySpecDecypt = DESKeySpec(password.toByteArray())
        var key2: Key  = kfDecypt.generateSecret(keySpecDecypt)
        cipherDecypt.init(Cipher.DECRYPT_MODE, key2)
    }

    //des加密
    fun encrypt(original: String): String {
        //加密/解密
        val encrypt = cipherEncypt.doFinal(original.toByteArray())
        //base64加密
        return String(Base64.encode(encrypt, Base64.DEFAULT))
    }

    //des解密，这里的original指的是加密后的原文
    fun decrypt(original: String): String {
        //base64解码
        val encrypt = cipherDecypt.doFinal(Base64.decode(original, Base64.DEFAULT))
        return String(encrypt)
    }
}
