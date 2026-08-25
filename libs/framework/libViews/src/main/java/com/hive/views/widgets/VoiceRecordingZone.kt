package com.hive.views.widgets

/**
 * 录音浮层的手势命中区域（抬起时三态）：
 * - CANCEL：左上取消区（红色高亮）→ 抬起取消
 * - SEND：右上发送区（绿色高亮）→ 抬起发送
 * - NEUTRAL：中间弧形区 → 抬起填写到 EditText
 */
enum class VoiceRecordingZone {
    NEUTRAL,
    CANCEL,
    SEND,
}

