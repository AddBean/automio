// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.driver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.base.core.ScriptThreadManager.delay
import com.hive.script.driver.ScriptEventHelper.Companion.get

/**
 * Helper class for text input operations in accessibility service
 */
object ScriptTextInputHelper {
    private const val TAG = "TextInputHelper"

    /**
     * Sets text content to an editable node using multiple fallback methods
     *
     * @param nodeInfo The accessibility node to set text on
     * @param content  The text content to set
     * @return Whether the text was successfully set
     */
    @JvmStatic
    fun setEditText(
        nodeInfo: AccessibilityNodeInfo?,
        animInput: Boolean,
        content: String?
    ): Boolean {
        if (nodeInfo == null || content == null) {
            Log.e(TAG, "节点或内容为空")
            return false
        }


        val beforeText = if (nodeInfo.text != null) nodeInfo.text.toString() else ""

        // Try all available methods in sequence until one succeeds
        return trySetTextAction(nodeInfo, animInput, content, beforeText) ||
                tryPasteAction(nodeInfo, content, beforeText) ||
                tryLongPressAndPaste(nodeInfo, content, beforeText) ||
                tryGestureClickAndSetText(nodeInfo, content, beforeText) ||
                tryCursorMovementAndPaste(nodeInfo, content, beforeText) ||
                tryAlternativeSetTextArgs(nodeInfo, content, beforeText)
    }

    /**
     * Appends text content to an editable node
     *
     * @param nodeInfo The accessibility node to append text to
     * @param content  The text content to append
     * @return Whether the text was successfully appended
     */
    @JvmStatic
    fun appendEditText(
        nodeInfo: AccessibilityNodeInfo,
        animInput: Boolean,
        content: String
    ): Boolean {
        var finalContent = content
        if (!TextUtils.isEmpty(nodeInfo.text)) {
            finalContent = nodeInfo.text.toString() + content
        }
        return setEditText(nodeInfo, animInput, finalContent)
    }

    /**
     * Gets a fresh instance of the node info by using performFindEditText
     *
     * @param nodeInfo The original accessibility node
     * @return A fresh instance of the node, or null if not found
     */
     fun getRefreshedNodeInfo(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (nodeInfo == null) {
            return null
        }

        val nodeInfos = get().performFindEditText(
            nodeInfo.viewIdResourceName, null, null
        )

        if (nodeInfos.isNullOrEmpty()) {
            return null
        }

        return nodeInfos.firstOrNull { it.isFocused } ?: nodeInfos[0]
    }

    /**
     * Checks if text was successfully set to the node
     *
     * @param nodeInfo        The accessibility node to check
     * @param expectedContent The expected text content
     * @return Whether the node contains the expected text
     */
    private fun isTextSetSuccessfully(
        nodeInfo: AccessibilityNodeInfo,
        expectedContent: String,
        beforeText: String
    ): Boolean {
        // Always get a fresh instance of the node to check current state
        val updatedNode = getRefreshedNodeInfo(nodeInfo)
        if (updatedNode == null) {
            Log.e(TAG, "无法获取更新后的节点信息")
            return false
        }

        try {
            // Get current text from node
            val currentText = updatedNode.text

            // Compare current text with expected text
            return currentText != null && !TextUtils.equals(beforeText, expectedContent)
        } catch (e: Exception) {
            Log.e(TAG, "验证文本设置时出错: " + e.message)
            return false
        }
    }

    /**
     * Method 1: Use ACTION_SET_TEXT to directly set text
     */
    private fun trySetTextAction(
        nodeInfo: AccessibilityNodeInfo,
        animInput: Boolean,
        content: String,
        beforeText: String
    ): Boolean {
        // Always get a fresh instance of the node
        val refreshedNode = performFocusAction(nodeInfo) ?: return false
        try {
            val result = performActionInput(refreshedNode, animInput, content)
            if (result) {
                return isTextSetSuccessfully(nodeInfo, content, beforeText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_SET_TEXT 异常: " + e.message)
        }
        return false
    }

    private fun performActionInput(
        nodeInfo: AccessibilityNodeInfo,
        animInput: Boolean,
        content: String
    ): Boolean {
        if (animInput) {
            var appendString=""
            //逐字输入
            for (char in content) {
                val arguments = Bundle()
                appendString += char
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    appendString.toString()
                )
                val result =
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (!result) {
                    return false
                }
                // 延时模拟打字效果
                ScriptThreadManager.delay(100)
            }
            return true
        } else {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                content
            )
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

    }


    /**
     * Method 2: Use clipboard and ACTION_PASTE to paste text
     */
    private fun tryPasteAction(
        nodeInfo: AccessibilityNodeInfo,
        content: String,
        beforeText: String
    ): Boolean {
        try {
            // Focus the node first

            var refreshedNode = performFocusAction(nodeInfo) ?: return false
            // Clear existing content if needed
            clearNodeText(refreshedNode)

            // Copy content to clipboard
            val service = accessibilityService ?: return false

            setClipboardText(service, content)

            // Get fresh node again after clipboard operations
            refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false

            // Execute paste operation
            if (refreshedNode.actionList != null) {
                val result = refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                if (result) {
                    return isTextSetSuccessfully(nodeInfo, content, beforeText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_PASTE 异常: " + e.message)
        }
        return false
    }

    /**
     * Method 3: Long press to show paste menu, then automatically paste
     */
    private fun tryLongPressAndPaste(
        nodeInfo: AccessibilityNodeInfo,
        content: String,
        beforeText: String
    ): Boolean {
        try {
            val service = accessibilityService ?: return false

            // Copy content to clipboard
            setClipboardText(service, content)

            // Focus the node
            var refreshedNode = performFocusAction(nodeInfo) ?: return false

            // Get node position on screen
            val bounds = Rect()
            refreshedNode.getBoundsInScreen(bounds)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 1. Long press to show context menu
                get().performPress(bounds.centerX(), bounds.centerY(), 1500)

                // 2. Wait for context menu to appear
                delay(1500)

                // 3. Try to find and click "paste" menu item
                val rootNode = service.rootInActiveWindow
                if (rootNode != null) {
                    // Try with various paste-related texts
                    val pasteTexts = arrayOf(
                        "粘贴", "黏贴", "貼上", "Paste", "paste", "PASTE",
                        "붙여넣기", "貼り付け", "Einfügen", "Coller", "Pegar", "Вставить"
                    )

                    var pasteNodes: List<AccessibilityNodeInfo>? = null

                    // Try all possible paste texts
                    for (pasteText in pasteTexts) {
                        pasteNodes = rootNode.findAccessibilityNodeInfosByText(pasteText)
                        if (!pasteNodes.isNullOrEmpty()) {
                            break
                        }
                    }

                    // If paste button found
                    if (!pasteNodes.isNullOrEmpty()) {
                        // Try clicking each found node
                        for (pasteNode in pasteNodes) {
                            val clickResult =
                                pasteNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            delay(300)
                            if (clickResult && isTextSetSuccessfully(
                                    nodeInfo,
                                    content,
                                    beforeText
                                )
                            ) {
                                return true
                            }
                        }
                    }

                    // Method C: Smart grid scanning
                    return scanAndClickAroundPoint(
                        service,
                        nodeInfo,
                        bounds.centerX().toFloat(),
                        bounds.centerY().toFloat(),
                        content,
                        beforeText
                    )
                }
            } else {
                // For lower Android versions, try using ACTION_LONG_CLICK
                refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
                val longClickResult =
                    refreshedNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                if (longClickResult) {
                    delay(500)

                    refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
                    val pasteResult =
                        refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    return pasteResult && isTextSetSuccessfully(
                        nodeInfo,
                        content,
                        beforeText
                    )
                }
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "长按粘贴菜单方法异常: " + e.message)
            return false
        }
    }

    /**
     * Method 4.1: Try gesture click followed by setText and paste
     */
    private fun tryGestureClickAndSetText(
        nodeInfo: AccessibilityNodeInfo,
        content: String, beforeText: String
    ): Boolean {
        try {
            val service = accessibilityService

            var refreshedNode = performFocusAction(nodeInfo) ?: return false
            // Get node bounds
            val bounds = Rect()
            refreshedNode.getBoundsInScreen(bounds)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use gesture click to ensure focus
                performGestureClick(service, bounds.centerX().toFloat(), bounds.centerY().toFloat())
                delay(300)

                // Try setting text again with fresh node
                refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    content
                )
                var result = refreshedNode.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )
                delay(500)

                if (result && isTextSetSuccessfully(nodeInfo, content, beforeText)) {
                    return true
                }

                // If direct text setting fails, try paste
                setClipboardText(service, content)

                // Get fresh node again and execute paste operation
                refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
                result = refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                if (result && isTextSetSuccessfully(nodeInfo, content, beforeText)) {
                    return true
                }

                // Click again and try paste with fresh node
                performGestureClick(
                    service,
                    bounds.centerX().toFloat(),
                    bounds.centerY().toFloat()
                )
                delay(500)

                refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
                result = refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                return result && isTextSetSuccessfully(nodeInfo, content, beforeText)
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "手势点击设置文本异常: " + e.message)
            return false
        }
    }

    /**
     * Method 4.2: Try cursor movement and paste operations
     */
    private fun tryCursorMovementAndPaste(
        nodeInfo: AccessibilityNodeInfo,
        content: String, beforeText: String
    ): Boolean {
        try {
            val service = accessibilityService

            var refreshedNode = performFocusAction(nodeInfo) ?: return false

            setClipboardText(service, content)

            // Get node bounds
            val bounds = Rect()
            refreshedNode.getBoundsInScreen(bounds)

            // Try long press to show context menu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                performGestureLongPress(
                    service,
                    bounds.centerX().toFloat(),
                    bounds.centerY().toFloat()
                )
                delay(500)
            }

            // Try paste again with fresh node
            refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
            val result = refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            return result && isTextSetSuccessfully(nodeInfo, content, beforeText)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "光标操作异常: " + e.message)
            return false
        }
    }

    /**
     * Method 4.3: Try alternative ways to set text with different argument keys
     */
    private fun tryAlternativeSetTextArgs(
        nodeInfo: AccessibilityNodeInfo,
        content: String,
        beforeText: String
    ): Boolean {
        try {
            var refreshedNode = performFocusAction(nodeInfo) ?: return false
            // Try variant 1 of ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE
            var setTextArgs = Bundle()
            setTextArgs.putString("ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE", content)
            var result =
                refreshedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs)

            if (result && isTextSetSuccessfully(nodeInfo, content, beforeText)) {
                return true
            }

            // Try variant 2 with fresh node
            refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false
            setTextArgs = Bundle()
            setTextArgs.putCharSequence(
                "android.view.accessibility.action.ARGUMENT_SET_TEXT_CHARSEQUENCE",
                content
            )
            result =
                refreshedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs)
            return result && isTextSetSuccessfully(nodeInfo, content, beforeText)
        } catch (e: Exception) {
            Log.e(TAG, "替代文本输入方法异常: " + e.message)
            return false
        }
    }

    /**
     * Scan and click around a point in a grid pattern, looking for paste menu
     */
    private fun scanAndClickAroundPoint(
        service: AccessibilityService,
        nodeInfo: AccessibilityNodeInfo,
        centerX: Float, centerY: Float,
        content: String, beforeText: String
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        try {
            // Always get a fresh instance of the node
            val refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return false

            // Get screen dimensions
            val displayBounds = Rect()
            refreshedNode.getBoundsInScreen(displayBounds)

            // Define scan grid parameters
            val gridSize = 5 // 5x5 grid
            val radius = 200 // scan radius
            val cellSize = radius * 2 / gridSize

            // Define priority order: scan top and bottom first, as paste menu usually appears there
            val scanOrder = arrayOf(
                intArrayOf(0, -1),
                intArrayOf(0, -2),
                intArrayOf(0, 1),
                intArrayOf(0, 2),  // top-bottom direction
                intArrayOf(-1, 0),
                intArrayOf(1, 0),
                intArrayOf(-1, -1),
                intArrayOf(1, -1),
                intArrayOf(-1, 1),
                intArrayOf(1, 1),  // left-right and diagonals
                intArrayOf(-2, -2),
                intArrayOf(-2, -1),
                intArrayOf(-2, 0),
                intArrayOf(-2, 1),
                intArrayOf(-2, 2),  // farther points
                intArrayOf(-1, -2),
                intArrayOf(-1, 2),
                intArrayOf(0, -3),
                intArrayOf(0, 3),
                intArrayOf(1, -2),
                intArrayOf(1, 2),
                intArrayOf(2, -2),
                intArrayOf(2, -1),
                intArrayOf(2, 0),
                intArrayOf(2, 1),
                intArrayOf(2, 2)
            )

            // Scan through points
            for (offset in scanOrder) {
                val x = centerX + offset[0] * cellSize
                val y = centerY + offset[1] * cellSize

                // Ensure point is within screen bounds
                if (x < 0 || y < 0 || x > displayBounds.width() || y > displayBounds.height()) {
                    continue
                }

                // Create click gesture
                val builder = GestureDescription.Builder()
                val path = Path()
                path.moveTo(x, y)
                builder.addStroke(StrokeDescription(path, 0, 50))

                // Execute click
                service.dispatchGesture(builder.build(), null, null)

                // Wait for click to take effect
                delay(300)

                // Verify if text has been set
                if (isTextSetSuccessfully(nodeInfo, content, beforeText)) {
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "网格扫描点击异常: " + e.message)
            return false
        }
    }

    /**
     * Clear text in a node
     */
    private fun clearNodeText(nodeInfo: AccessibilityNodeInfo?) {
        if (nodeInfo == null) {
            return
        }

        // Always get a fresh instance of the node
        var refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
            !TextUtils.isEmpty(refreshedNode.text)
        ) {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                ""
            )
            refreshedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            // Get fresh node again to check if text was cleared
            refreshedNode = getRefreshedNodeInfo(nodeInfo) ?: return
            if (!TextUtils.isEmpty(refreshedNode.text)) {
                // Move to text start
                refreshedNode.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)

                // Select all text
                val selArgs = Bundle()
                selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                selArgs.putInt(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                    refreshedNode.text.length
                )
                refreshedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)

                // Delete selected text
                refreshedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
            }
        }
    }

    /**
     * Set text to clipboard
     */
    private fun setClipboardText(service: AccessibilityService?, content: String) {
        val clipboard = service!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", content)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Perform a gesture click at specified coordinates
     */
    private fun performGestureClick(service: AccessibilityService?, x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(x, y)
            builder.addStroke(StrokeDescription(path, 0, 50)) // 50ms click
            service!!.dispatchGesture(builder.build(), null, null)
        }
    }

    /**
     * Perform a gesture long press at specified coordinates
     */
    private fun performGestureLongPress(service: AccessibilityService?, x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(x, y)
            builder.addStroke(StrokeDescription(path, 0, 500)) // 500ms long press
            service!!.dispatchGesture(builder.build(), null, null)
        }
    }

    private val accessibilityService: AccessibilityService?
        /**
         * Get the accessibility service instance
         */
        get() {
            val service: AccessibilityService? = get().getAccessService()
            if (service == null) {
                Log.e(TAG, "无法获取 AccessibilityService")
            }
            return service
        }

    private fun performFocusAction(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (nodeInfo == null) {
            return null
        }

        try {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            delay(700)
            return getRefreshedNodeInfo(nodeInfo)
        } catch (e: Exception) {
            Log.e(TAG, "执行焦点操作异常: " + e.message)
            return null
        }
    }
}
