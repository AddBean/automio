// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extends

import java.util.*

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/16/21
 */
fun <E> MutableList<E>.toLinkedList(): LinkedList<E> {
    var linkedList = LinkedList<E>()
    forEach {
        linkedList.add(it)
    }
    return linkedList
}