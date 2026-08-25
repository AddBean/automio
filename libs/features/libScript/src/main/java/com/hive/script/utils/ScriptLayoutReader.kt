// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.utils

import android.graphics.Rect
import android.text.TextUtils
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.annotation.NotProguard
import com.hive.script.driver.ScriptEventHelper
import com.hive.utils.extends.toDigits

@NotProguard
object ScriptLayoutReader {

    // 防止栈溢出的限制参数
    private const val MAX_RECURSION_DEPTH = 10
    private const val MAX_NODE_COUNT = 1000

    fun getCurrentLayout(): LayoutResult {
        val scriptEventHelper = ScriptEventHelper.get()
        val serviceEntity = scriptEventHelper.serviceEntity ?: return LayoutResult(false)
        val rootNode = serviceEntity.rootInActiveWindow ?: return LayoutResult(false)

        val screenWidth = ScriptCoordinateAdapter.getScreenWidthByOrientation()
        val screenHeight = ScriptCoordinateAdapter.getScreenHeightByOrientation()

        var nodeCount = 0

        fun convertNodeToLayoutInfo(node: AccessibilityNodeInfo, depth: Int = 0): LayoutNodeInfo? {
            // 防止栈溢出：检查递归深度和节点数量
            if (depth > MAX_RECURSION_DEPTH || nodeCount > MAX_NODE_COUNT) {
                return null
            }

            nodeCount++

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // 转换为归一化坐标 (0-1)
            val boundsInfo = BoundsInfo(
                l = (if (screenWidth > 0) bounds.left.toFloat() / screenWidth else 0f).toDigits(4),
                t = (if (screenHeight > 0) bounds.top.toFloat() / screenHeight else 0f).toDigits(4),
                r = (if (screenWidth > 0) bounds.right.toFloat() / screenWidth else 0f).toDigits(4),
                b = (if (screenHeight > 0) bounds.bottom.toFloat() / screenHeight else 0f).toDigits(
                    4
                ),
            )

            val centerPoint = CenterPoint(
                x = (if (screenWidth > 0) bounds.centerX()
                    .toFloat() / screenWidth else 0f).toDigits(4),
                y = (if (screenHeight > 0) bounds.centerY()
                    .toFloat() / screenHeight else 0f).toDigits(4)
            )

            val children = mutableListOf<LayoutNodeInfo>()
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    if (childNode.isVisibleToUser) {
                        convertNodeToLayoutInfo(childNode, depth + 1)?.let { childInfo ->
                            children.add(childInfo)
                        }
                    }
                }
            }

            return LayoutNodeInfo(
                id = if (!TextUtils.isEmpty(node.viewIdResourceName)) node.viewIdResourceName else null,
                text = if (!TextUtils.isEmpty(node.text)) node.text.toString().take(100) else null,
                desc = if (!TextUtils.isEmpty(node.contentDescription)) node.contentDescription.toString() else null,
                clazz = node.className?.toString()?.substringAfterLast('.'),
                click = "${centerPoint.x},${centerPoint.y}",
                bounds = boundsInfo,
                clickable = node.isClickable,
                checked = node.isChecked,
                visible = node.isVisibleToUser,
//                bounds = boundsInfo,
//                clickable = node.isClickable,
//                scrollable = node.isScrollable,
//                checkable = node.isCheckable,
//                enabled = node.isEnabled,
//                focusable = node.isFocusable,
                packageName = node.packageName?.toString(),
                nodes = children
            )
        }

        val rootLayoutInfo = convertNodeToLayoutInfo(rootNode)
            ?: return LayoutResult(
                success = false,
                pkgName = rootNode.packageName?.toString(),
                width = screenWidth,
                height = screenHeight,
                nodeCount = nodeCount
            )

        // 如果根节点转换失败，返回失败结果

        return LayoutResult(
            success = true,
            pkgName = rootNode.packageName?.toString(),
            width = screenWidth,
            height = screenHeight,
            rootNode = rootLayoutInfo,
            nodeCount = nodeCount
        )
    }


    data class LayoutNodeInfo(
        val clazz: String? = null,
        val text: String? = null,
        val desc: String? = null,
        val id: String? = null,
        val bounds: BoundsInfo? = null,
        val click: String? = null,
        val clickable: Boolean = false,
//        val scrollable: Boolean = false,
//        val checkable: Boolean = false,
        val checked: Boolean = false,
//        val enabled: Boolean = false,
//        val focusable: Boolean = false,
        val visible: Boolean = false,
        val packageName: String? = null,
        val nodes: List<LayoutNodeInfo> = emptyList()
    )


    data class CenterPoint(
        val x: Float,  // 归一化坐标 0-1
        val y: Float,  // 归一化坐标 0-1
    )

    data class BoundsInfo(
        val l: Float,     // 归一化坐标 0-1
        val t: Float,      // 归一化坐标 0-1
        val r: Float,    // 归一化坐标 0-1
        val b: Float,   // 归一化坐标 0-1
    )

    data class LayoutResult(
        val success: Boolean,
        val pkgName: String? = null,
        val width: Int = 0,
        val height: Int = 0,
        val rootNode: LayoutNodeInfo? = null,
        val nodeCount: Int = 0
    )
}
