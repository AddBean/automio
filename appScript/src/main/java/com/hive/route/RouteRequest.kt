// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.route

data class RouteRequest(
    val page: String,
    val params: Map<String, String> = emptyMap(),
    val originalUri: String? = null
)
