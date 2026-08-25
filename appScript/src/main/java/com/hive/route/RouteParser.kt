// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.route

import android.net.Uri

object RouteParser {
    private const val PARAM_PAGE = "page"

    fun parse(uri: Uri?): RouteRequest? {
        if (uri == null) return null
        val page = uri.getQueryParameter(PARAM_PAGE)
            ?: uri.pathSegments.firstOrNull()
            ?: return null
        val params = linkedMapOf<String, String>()
        for (name in uri.queryParameterNames) {
            val value = uri.getQueryParameter(name) ?: continue
            params[name] = value
        }
        return RouteRequest(page = page, params = params, originalUri = uri.toString())
    }

    fun parse(rawUri: String?): RouteRequest? {
        if (rawUri.isNullOrBlank()) return null
        return runCatching { parse(Uri.parse(rawUri)) }.getOrNull()
    }
}
