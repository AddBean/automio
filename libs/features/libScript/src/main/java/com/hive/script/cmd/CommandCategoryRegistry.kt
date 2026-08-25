// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.cmd

import com.hive.i8n.R as I8nR
import com.hive.script.R

/**
 * 命令插入类型常量，与 [CommandCategoryRegistry] 配合使用
 */
object CmdInsertType {
    const val TYPE_TITLE = -1
    const val TYPE_INSERT_LOOP = 1
    const val TYPE_INSERT_IF = 2
    const val TYPE_INSERT_RECORD = 3
    const val TYPE_INSERT_SCRIPT = 4
    const val TYPE_INSERT_DELAY = 5
    const val TYPE_INSERT_OPEN_APP = 6
    const val TYPE_INSERT_OPEN_LINK = 7
    const val TYPE_INSERT_COPY = 8
    const val TYPE_INSERT_EXIT = 9
    const val TYPE_INSERT_TOAST = 10
    const val TYPE_INSERT_UNLOCK = 11
    const val TYPE_INSERT_LOCK = 12
    const val TYPE_INSERT_BREAK = 13
    const val TYPE_INSERT_ACTION_SNAPSHOT = 14
    const val TYPE_INSERT_ACTION_BACK = 15
    const val TYPE_INSERT_ACTION_HOME = 16
    const val TYPE_INSERT_ACTION_RECENT = 17
    const val TYPE_INSERT_ACTION_NOTIFICATION = 18
    const val TYPE_INSERT_CLICK_IMAGE = 19
    const val TYPE_INSERT_CLICK_VIEW = 20
    const val TYPE_INSERT_CLICK_COLOR = 21
    const val TYPE_INSERT_INPUT = 22
    const val TYPE_INSERT_BATCH_CLICK = 23
    const val TYPE_INSERT_CLICK_OR_SCROLL = 24
    const val TYPE_INSERT_FAST_CLICK = 25
    const val TYPE_INSERT_TIMER_CALIBRATOR = 26
    const val TYPE_INSERT_SCROLL_MULTIPLE = 27
    const val TYPE_INSERT_SCALE_IN_OUT = 28
    const val TYPE_INSERT_PLAY_AUDIO = 29
    const val TYPE_INSERT_JUMP = 31
    const val TYPE_INSERT_JUMP_POINT = 32
    const val TYPE_INSERT_LOAD_SCRIPT = 33
    const val TYPE_INSERT_CLICK_TEXT = 34
    const val TYPE_INSERT_READ_SCREEN_TEXT = 35
    const val TYPE_INSERT_SET_PARAM = 36
    const val TYPE_INSERT_CURL = 37
    const val TYPE_INSERT_READ_VIEW_TEXT = 38
    const val TYPE_INSERT_AI_REQUEST = 39
    const val TYPE_INSERT_DOWNLOAD = 40
    const val TYPE_INSERT_RUN_SKILL = 41
    const val TYPE_INSERT_PYTHON_EXECUTOR = 42
    const val TYPE_INSERT_VOICE_TTS = 43
    const val TYPE_INSERT_VOICE_ASR = 44
}

/**
 * 命令分类，用于 UI 分组展示
 */
object CmdCategory {
    const val LOGIC_CONTROL = 1       // 逻辑控制
    const val INSERT_REFERENCE = 2    // 插入引用
    const val COMMON = 7              // 常用指令
    const val AUTO_RECOGNITION = 3    // 自动识别
    const val TEXT_OPERATION = 4      // 文字操作
    const val GESTURE = 5             // 手势指令
    const val SYSTEM = 6              // 系统指令
    const val ADVANCED = 8            // 高级指令

    /** 展示顺序（与 const 数值顺序不一致，按 UI 设计） */
    val orderedIds: List<Int> = listOf(
        LOGIC_CONTROL,
        INSERT_REFERENCE,
        COMMON,
        AUTO_RECOGNITION,
        TEXT_OPERATION,
        GESTURE,
        SYSTEM,
        ADVANCED
    )

    private val titleResMap: Map<Int, Int> = mapOf(
        LOGIC_CONTROL to I8nR.string.sc_edit_insert_type_title_n_1,
        INSERT_REFERENCE to I8nR.string.sc_edit_insert_type_title_n_2,
        COMMON to I8nR.string.sc_edit_insert_type_title_7,
        AUTO_RECOGNITION to I8nR.string.sc_edit_insert_type_title_n_3,
        TEXT_OPERATION to I8nR.string.sc_edit_insert_type_title_n_4,
        GESTURE to I8nR.string.sc_edit_insert_type_title_n_5,
        SYSTEM to I8nR.string.sc_edit_insert_type_title_n_6,
        ADVANCED to I8nR.string.sc_edit_insert_type_title_n_8
    )

    fun getTitleResId(categoryId: Int): Int =
        titleResMap[categoryId] ?: I8nR.string.sc_edit_insert_type_title_7
}

/**
 * 命令插入项元数据，用于 [CommandCategoryRegistry]
 */
data class CmdInsertItemMeta(
    val addType: Int,
    val stringResId: Int,
    val drawableResId: Int,
    val cmdId: Int?
)

/**
 * 命令分类注册表，通过 Map 管理分类与命令的对应关系
 */
object CommandCategoryRegistry {

    private fun item(addType: Int, strRes: Int, drawRes: Int, cmdId: Int?) =
        CmdInsertItemMeta(addType, strRes, drawRes, cmdId)

    private val categoryToItems: Map<Int, List<CmdInsertItemMeta>> = mapOf(
        CmdCategory.LOGIC_CONTROL to listOf(
            item(CmdInsertType.TYPE_INSERT_IF, I8nR.string.cmd_ctr_menu_if, R.drawable.sc_icon_if, IDS.CmdIf),
            item(CmdInsertType.TYPE_INSERT_EXIT, I8nR.string.cmd_ctr_menu_exit, R.drawable.sc_icon_exit, IDS.CmdExit),
            item(CmdInsertType.TYPE_INSERT_LOOP, I8nR.string.cmd_ctr_menu_for, R.drawable.sc_icon_circly, IDS.CmdFor),
            item(CmdInsertType.TYPE_INSERT_BREAK, I8nR.string.cmd_ctr_menu_for_break, R.drawable.sc_icon_circly_break, IDS.CmdBreak),
            item(CmdInsertType.TYPE_INSERT_JUMP_POINT, I8nR.string.cmd_ctr_menu_jump_point, R.drawable.sc_cmd_jump_point, IDS.CmdJumpPoint),
            item(CmdInsertType.TYPE_INSERT_JUMP, I8nR.string.cmd_ctr_menu_jump, R.drawable.sc_cmd_jump, IDS.CmdJump)
        ),
        CmdCategory.INSERT_REFERENCE to listOf(
            item(CmdInsertType.TYPE_INSERT_SCRIPT, I8nR.string.sc_edit_add_group, R.drawable.sc_icon_scriptfile, null),
            item(CmdInsertType.TYPE_INSERT_RECORD, I8nR.string.sc_edit_record_cmd, R.drawable.sc_icon_record, null),
            item(CmdInsertType.TYPE_INSERT_LOAD_SCRIPT, I8nR.string.sc_edit_load_script_cmd, R.drawable.sc_cmd_import_script, IDS.CmdCallScript),
            item(CmdInsertType.TYPE_INSERT_RUN_SKILL, I8nR.string.sc_edit_run_skill_cmd, R.drawable.sc_cmd_run_skill, IDS.CmdRunSkill)
        ),
        CmdCategory.COMMON to listOf(
            item(CmdInsertType.TYPE_INSERT_DELAY, I8nR.string.cmd_ctr_menu_delay, R.drawable.sc_icon_delay, IDS.CmdDelay),
            item(CmdInsertType.TYPE_INSERT_OPEN_APP, I8nR.string.cmd_ctr_menu_open_app, R.drawable.sc_icon_app, IDS.CmdOpenApp),
            item(CmdInsertType.TYPE_INSERT_OPEN_LINK, I8nR.string.cmd_ctr_menu_open_link, R.drawable.sc_icon_link, IDS.CmdOpenUrl),
            item(CmdInsertType.TYPE_INSERT_UNLOCK, I8nR.string.cmd_ctr_menu_unlock, R.drawable.sc_icon_unlock, IDS.CmdUnlock),
            item(CmdInsertType.TYPE_INSERT_LOCK, I8nR.string.cmd_ctr_menu_lock, R.drawable.ic_lock, IDS.CmdActionScreenLock),
            item(CmdInsertType.TYPE_INSERT_ACTION_NOTIFICATION, I8nR.string.cmd_ctr_menu_notification, R.drawable.ic_notify, IDS.CmdActionOpenNotifications),
            item(CmdInsertType.TYPE_INSERT_TIMER_CALIBRATOR, I8nR.string.cmd_name_time_calibrator, R.drawable.sc_time_calibrator, IDS.CmdAlignToSecond),
            item(CmdInsertType.TYPE_INSERT_TOAST, I8nR.string.cmd_ctr_menu_toast, R.drawable.sc_icon_toast, IDS.CmdToast),
            item(CmdInsertType.TYPE_INSERT_PLAY_AUDIO, I8nR.string.cmd_name_play_audio, R.drawable.sc_cmd_reminder, IDS.CmdPlayAudio)
        ),
        CmdCategory.AUTO_RECOGNITION to listOf(
            item(CmdInsertType.TYPE_INSERT_CLICK_IMAGE, I8nR.string.cmd_ctr_menu_image, R.drawable.ic_check_pic, IDS.CmdClickImage),
            item(CmdInsertType.TYPE_INSERT_CLICK_TEXT, I8nR.string.cmd_ctr_menu_text, R.drawable.ic_text_setting, IDS.CmdClickText),
            item(CmdInsertType.TYPE_INSERT_CLICK_VIEW, I8nR.string.cmd_ctr_menu_layout, R.drawable.ic_layout, IDS.CmdClickView),
            item(CmdInsertType.TYPE_INSERT_CLICK_COLOR, I8nR.string.cmd_ctr_menu_color, R.drawable.ic_color_setting, IDS.CmdClickColor)
        ),
        CmdCategory.TEXT_OPERATION to listOf(
            item(CmdInsertType.TYPE_INSERT_INPUT, I8nR.string.cmd_name_input, R.drawable.ic_input, IDS.CmdInput),
            item(CmdInsertType.TYPE_INSERT_COPY, I8nR.string.cmd_ctr_menu_copy_clip, R.drawable.sc_icon_paste, IDS.CmdCopyToClipboard),
            item(CmdInsertType.TYPE_INSERT_READ_SCREEN_TEXT, I8nR.string.cmd_ctr_menu_read_screen_text, R.drawable.sc_ic_ocr_read, IDS.CmdReadScreenText),
            item(CmdInsertType.TYPE_INSERT_READ_VIEW_TEXT, I8nR.string.cmd_ctr_menu_read_view_text, R.drawable.sc_icon_view_text, IDS.CmdReadViewText),
            item(CmdInsertType.TYPE_INSERT_VOICE_TTS, I8nR.string.cmd_voice_tts_name, R.drawable.sc_ic_dialogue, IDS.CmdVoiceInteract),
            item(CmdInsertType.TYPE_INSERT_VOICE_ASR, I8nR.string.cmd_voice_asr_name, R.drawable.sc_ic_dialogue, IDS.CmdVoiceInteract)
        ),
        CmdCategory.GESTURE to listOf(
            item(CmdInsertType.TYPE_INSERT_CLICK_OR_SCROLL, I8nR.string.cmd_ctr_menu_click_scroll, R.drawable.ic_click, IDS.CmdClick),
            item(CmdInsertType.TYPE_INSERT_BATCH_CLICK, I8nR.string.cmd_name_batch_click, R.drawable.ic_grid, IDS.CmdPatternTap),
            item(CmdInsertType.TYPE_INSERT_FAST_CLICK, I8nR.string.cmd_ctr_menu_fast_click, R.drawable.ic_fast_click, IDS.CmdRepeatTap),
            item(CmdInsertType.TYPE_INSERT_SCALE_IN_OUT, I8nR.string.cmd_des_scale, R.drawable.ic_touch_small, IDS.CmdPinchZoom),
            item(CmdInsertType.TYPE_INSERT_SCROLL_MULTIPLE, I8nR.string.cmd_name_scroll_multiple, R.drawable.ic_fingger, IDS.CmdScrollMultiple)
        ),
        CmdCategory.SYSTEM to listOf(
            item(CmdInsertType.TYPE_INSERT_ACTION_SNAPSHOT, I8nR.string.cmd_ctr_menu_snapshot, R.drawable.ic_screen_cut, IDS.CmdActionScreenShot),
            item(CmdInsertType.TYPE_INSERT_ACTION_BACK, I8nR.string.cmd_ctr_menu_back, R.drawable.ic_roll_back, IDS.CmdActionBack),
            item(CmdInsertType.TYPE_INSERT_ACTION_HOME, I8nR.string.cmd_ctr_menu_home, R.drawable.ic_menu_home, IDS.CmdActionHome),
            item(CmdInsertType.TYPE_INSERT_ACTION_RECENT, I8nR.string.cmd_ctr_menu_recent, R.drawable.ic_recent, IDS.CmdActionRecent)
        ),
        CmdCategory.ADVANCED to listOf(
            item(CmdInsertType.TYPE_INSERT_SET_PARAM, I8nR.string.cmd_ctr_menu_set, R.drawable.sc_icon_param, IDS.CmdSet),
            item(CmdInsertType.TYPE_INSERT_CURL, I8nR.string.cmd_curl_name, R.drawable.sc_icon_curl, IDS.CmdCurl),
            item(CmdInsertType.TYPE_INSERT_DOWNLOAD, I8nR.string.cmd_download_name, R.drawable.sc_icon_download_cmd, IDS.CmdDownload),
            item(CmdInsertType.TYPE_INSERT_PYTHON_EXECUTOR, I8nR.string.script_python_executor_name, R.drawable.ic_sc_root, IDS.CmdPythonExecutor),
            item(CmdInsertType.TYPE_INSERT_AI_REQUEST, I8nR.string.cmd_ctr_menu_ai_request, R.drawable.sc_ai_request, IDS.CmdAiRequest)
        )
    )

    /** 按展示顺序的完整条目列表 */
    val orderedCategoryEntries: List<Pair<Int, List<CmdInsertItemMeta>>> =
        CmdCategory.orderedIds.mapNotNull { id -> categoryToItems[id]?.let { id to it } }

    /** cmdId -> 分类 */
    val cmdIdToCategory: Map<Int, Int> = orderedCategoryEntries.flatMap { (catId, items) ->
        items.mapNotNull { it.cmdId?.let { cmdId -> cmdId to catId } }
    }.toMap()

    /** addType -> 分类 */
    val addTypeToCategory: Map<Int, Int> = orderedCategoryEntries.flatMap { (catId, items) ->
        items.map { it.addType to catId }
    }.toMap()

    fun getItemsByCategory(categoryId: Int): List<CmdInsertItemMeta> =
        categoryToItems[categoryId].orEmpty()

    fun getCategoryByCmdId(cmdId: Int): Int? = cmdIdToCategory[cmdId]

    fun getCategoryByAddType(addType: Int): Int? = addTypeToCategory[addType]
}

/**
 * 扩展：根据命令 ID 获取所属分类
 */
fun Int.asCmdCategory(): Int? = CommandCategoryRegistry.getCategoryByCmdId(this)
