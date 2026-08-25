// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base

import androidx.annotation.IntDef
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptPermissionManager
import com.hive.utils.BaseConst
import com.hive.utils.GlobalApp
import com.hive.utils.LanguageManager
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.extends.string
import com.hive.utils.file.FileUtils
import kotlin.random.Random

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/8/21
 */
object ScriptConst {

    val Anim_Duration = 300L

    // 执行类：采用更柔和的莫兰迪色系，避免“红绿灯”既视感
    val colorCmdArray = arrayOf(
        0xFFDDBEA9.toInt(), // 浅原木
        0xFF8D99AE.toInt(), // 灰蓝
        0xFFE6BEBB.toInt(), // 灰调浅粉
        0xFF6B705C.toInt(), // 橄榄绿
        0xFFA2D2FF.toInt(), // 浅雾蓝
        0xFFE9C46A.toInt(), // 柔和金
        0xFFB5838D.toInt(), // 烟熏豆沙
        0xFFCAD2C5.toInt(), // 浅灰绿
        0xFF6D597A.toInt(), // 暮紫
        0xFFF8EDEB.toInt(), // 极淡粉灰
        0xFF457B9D.toInt(), // 钢蓝
        0xFFF4A261.toInt(), // 砂岩橙
        0xFFB7B7A4.toInt(), // 灰褐色
        0xFFE5989B.toInt(), // 柔粉
        0xFF98C1D9.toInt(), // 冰粉蓝
        0xFFD4A373.toInt(), // 琥珀色
        0xFF588157.toInt(), // 灰抹绿
        0xFFD5BDAF.toInt(), // 灰褐
        0xFFBDE0FE.toInt(), // 冰川蓝
        0xFFE56B6F.toInt(), // 珊瑚色
        0xFFA5A58D.toInt(), // 亚麻青
        0xFF6096BA.toInt(), // 钢青
        0xFFD4A5A5.toInt(), // 暮色玫瑰
        0xFFF5EBE0.toInt(), // 亚麻白
        0xFF84A59D.toInt(), // 灰青色
        0xFFFFB4A2.toInt(), // 奶油肉粉
        0xFF274C77.toInt(), // 深海灰
        0xFFE76F51.toInt(), // 砖陶红
        0xFFCB997E.toInt(), // 浅褐
        0xFFB56576.toInt(), // 灰紫
        0xFFA3B18A.toInt(), // 鼠尾草绿
        0xFFE3D5CA.toInt(), // 暖灰
        0xFF9D8189.toInt(), // 灰紫红
        0xFFA8DADC.toInt(), // 氧气蓝
        0xFFFFE5D9.toInt(), // 奶油杏
        0xFFEDAFB8.toInt(), // 浅樱花
        0xFFB5A4A3.toInt(), // 灰泥色
        0xFFD6CCC2.toInt(), // 鹅卵石
        0xFFA29BFE.toInt(), // 灰调丁香
        0xFF432818.toInt()  // 焦糖黑
    )

    /**
     * 按 CmdCategory 的色系，同分类使用相近色，便于视觉区分
     * Key: CmdCategory.xxx, Value: 该分类的色板（同分类内微变）
     */
    val colorByCategory: Map<Int, IntArray> = mapOf(
        1 to intArrayOf(
            0xFF6D597A.toInt(),
            0xFF8338EC.toInt(),
            0xFF9D8189.toInt()
        ),           // 逻辑控制-紫系
        2 to intArrayOf(
            0xFF457B9D.toInt(),
            0xFF274C77.toInt(),
            0xFF98C1D9.toInt()
        ),           // 插入引用-蓝系
        7 to intArrayOf(
            0xFFE9C46A.toInt(),
            0xFFF4A261.toInt(),
            0xFFD4A373.toInt()
        ),           // 常用指令-金橙系
        3 to intArrayOf(
            0xFF6B705C.toInt(),
            0xFF588157.toInt(),
            0xFFA3B18A.toInt()
        ),           // 自动识别-绿系
        4 to intArrayOf(
            0xFFE6BEBB.toInt(),
            0xFFD4A5A5.toInt(),
            0xFFE5989B.toInt()
        ),           // 文字操作-粉系
        5 to intArrayOf(
            0xFFA2D2FF.toInt(),
            0xFFBDE0FE.toInt(),
            0xFFA8DADC.toInt()
        ),           // 手势指令-蓝绿系
        6 to intArrayOf(
            0xFFCAD2C5.toInt(),
            0xFFB7B7A4.toInt(),
            0xFFD6CCC2.toInt()
        ),           // 系统指令-灰系
        8 to intArrayOf(
            0xFF274C77.toInt(),
            0xFF432818.toInt(),
            0xFF457B9D.toInt()
        )           // 高级指令-深色系
    )

    // 跳转类：侧重于逻辑感，使用偏冷的青、绿、紫
    val colorJumpArray = arrayOf(
        0xFF588157.toInt(), // 森绿
        0xFF3A5A40.toInt(), // 深绿
        0xFF219EBC.toInt(), // 亮蓝
        0xFF023047.toInt(), // 深海蓝
        0xFF8338EC.toInt(), // 炫目紫
        0xFF3A86FF.toInt()  // 纯真蓝
    )

    val colorPickArray = arrayOf(
        0xFFffffff.toInt(),
        0xFFA593E0.toInt(),
        0xFF9DC8C8.toInt(),
        0xFF58C9B9.toInt(),
        0xFF519D9E.toInt(),
        0xFF30A9DE.toInt(),
        0xFFEFDC05.toInt(),
        0xFFE53A40.toInt(),
        0xFF090707.toInt()
    )

    val colorParamSys = 0xffC25FFF.toInt()

    val colorParam = 0XFF018671.toInt()

    var supportImport = true

    var runningDialogShow = false

    var scriptStepIndex = 0

    var scriptLoopCount = 1

    val Script_File_Suffix = ".zip"

    val SCRIPT_SUFFIX = ".jds"

    val SCRIPT_SUFFIX_ENCRYPT = ".encrypt"

    val SCRIPT_SUFFIX_INFO = ".info"

    val LAYOUT_SUFFIX = ".layout"

    val SCRIPT_MAIN_FILE_NAME = "main$SCRIPT_SUFFIX"

    val SCRIPT_MAIN_ENCRYPT_FILE_NAME = "main$SCRIPT_SUFFIX_ENCRYPT"

    val SCRIPT_MAIN_INFO_FILE_NAME = "main$SCRIPT_SUFFIX_INFO"

    val SCRIPT_LAYOUT_FILE_NAME = "main$LAYOUT_SUFFIX"

    /** 自定义脚本型 tool 的 ID 前缀。格式：custom.<scriptUid> */
    const val SCRIPT_TOOL_ID_PREFIX: String = "custom."

    var NONE_CHAR = "-"

    var Save_Import_Temp_Path = "${BaseConst.getBaseDir()}/import_temp/"

    var Save_Share_Path = "${BaseConst.getBaseDir()}/share/"

    var Save_Share_Temp_Path = "${BaseConst.getBaseDir()}/share_temp/"

    var Save_Script_Path = "${BaseConst.getBaseDir()}/script/"

    /** 独立安装的 tool 根目录（bundle primaryType=tool 时安装到此），与 script 并列 */
    var Save_Tool_Path = "${BaseConst.getBaseDir()}/tools/"

    /** 独立安装的 public skill 根目录（市场显式下载、本地创建），与 script 并列 */
    var Save_Skill_Path = "${BaseConst.getBaseDir()}/skills/"

    const val SKILL_FILE_SUFFIX: String = ".skill"

    var Default_file_Name = GlobalApp.getString(com.hive.i8n.R.string.script_default_name)

    var Save_Script_Temp_Path = "${BaseConst.getBaseDir()}/script/temp/"

    var Save_Script_Temp_Image_Path = "${BaseConst.getBaseDir()}/temp/image/"

    var Save_Image_Relative_Path = "./images/"

    const val Task_Screen_Unlock_Script_Name = ".default_user_unlock_screen_task"

    var Task_Screen_Lock_Script_Main_Path =
        "$Save_Script_Path/$Task_Screen_Unlock_Script_Name/"

    var Mate_Version = 3

    var Cmd_Spot_Accuracy = 0.85

    var Cmd_Spot_Normal = 0.75

    var Cmd_Spot_Obscure = 0.65

    var Cmd_Spot_Color_Threshold = 10

    val Cmd_Spot_Color_Threshold_Accuracy = 5;

    val Cmd_Spot_Color_Threshold_Normal = 10;

    val Cmd_Spot_Color_Threshold_Obscure = 20;

    var Cmd_Default_Delay = 100L

    var Cmd_Default_Bias = 10L

    var Cmd_Default_Base = 100L

    var Cmd_Default_Capture_Camera = 300L

    var Cmd_Default_Capture = 200L

    var Cmd_Default_Play_Audio = 3000L

    var Cmd_Wakeup_Default = 100L

    var Cmd_Click_Default = 15L

    var Cmd_Default_Spot = 200L

    var Cmd_Default_Spot_Color = 200L

    var Cmd_Default_Spot_Text = 200L

    var Cmd_Default_OpenScheme = 500L

    var Cmd_Default_OpenApp = 500L

    var Cmd_Delay_Default = 300L

    var Cmd_Drag_DURATION = 2000L

    var Cmd_Default = 5L

    var Cmd_Default_AI = 2000L

    var Cmd_Screen_Lock_Delay_Default = 50L

    var Cmd_Long_Click_Default = 1500L

    var Cmd_Scroll_Click_Default = 1500L

    var Cmd_Fast_Click_Gap_Default = 200L

    var Cmd_Fast_Click_Count_Default = 10

    var Cmd_Click_Layout_Default = 60L

    var Cmd_Click_Scale_Default = 1000L

    var Cmd_Click_Multiple_Default = 1000L

    var Default_Anti_Detect_Radius_Value = 4

    var Cmd_Default_Unlock_Screen_Delay = 1000L

    var Cmd_Default_Condition_Delay = 0L

    var Cmd_Default_If_Delay = 500L

    val SCRIPT_SP_APP_PERMISSION_GRANT = "script_sp_app_permission_grant"

    var Access_Help_Url = "https://www.baidu.com"

    var Filter_Script_Tag: String? = null

    fun newMd5RelativePath(path: String?): String {
        return "$Save_Image_Relative_Path${Md5Utils.file2md5(path)}.png"
    }

    fun newRandomFullPath(): String {
        return "$Save_Script_Temp_Path${System.currentTimeMillis()}R${Random(1000).nextInt(1000)}.png"
    }

    fun newRandomTempImagePath(extension: String): String {
        FileUtils.makeDirs(Save_Script_Temp_Image_Path)
        return "$Save_Script_Temp_Image_Path${System.currentTimeMillis()}R${
            Random(1000).nextInt(
                1000
            )
        }.${extension}"
    }

    object From {
        const val FROM_SCRIPT_UNKNOWN = -1
        const val FROM_SCRIPT_LIST = 0
        const val FROM_SCRIPT_LIST_NEW = FROM_SCRIPT_LIST + 1
        const val FROM_SCRIPT_MENU_MAIN = FROM_SCRIPT_LIST_NEW + 1
        const val FROM_SCRiPT_SCRIPT_CONFIRM_PAGE = FROM_SCRIPT_MENU_MAIN + 1
    }

    @IntDef(
        From.FROM_SCRIPT_UNKNOWN,
        From.FROM_SCRIPT_LIST,
        From.FROM_SCRIPT_MENU_MAIN,
        From.FROM_SCRiPT_SCRIPT_CONFIRM_PAGE
    )
    internal annotation class FromSource

    val RegexActions = listOf(
        //提取前100个字符
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_0.string(),
            """(.{0,100})"""
        ),
        //提取数字
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_1.string(),
            """(\d+)"""
        ),
        //提取手机号码
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_2.string(),
            """(1[3-9]\d{9})"""
        ),
        //提取邮箱(包括纯数字)
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_3.string(),
            """([a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+)"""
        ),
        //提取网址
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_4.string(),
            """([a-zA-z]+://[^\s]*)"""
        ),
        //邮政编号
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_5.string(),
            """([1-9]\d{5})"""
        ),
        //提取中文
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_6.string(),
            """([\u4e00-\u9fa5]+)"""
        ),
        //提取身份证号码
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_7.string(),
            """(\d{17}[\d|x]|\d{15})"""
        ),
        //提取IP地址
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_8.string(),
            """(\d+\.\d+\.\d+\.\d+)"""
        ),
        //提取日期
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_9.string(),
            """(\d{4}-\d{1,2}-\d{1,2})"""
        ),
        //提取时间
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_10.string(),
            """(\d{1,2}:\d{1,2}:\d{1,2})"""
        ),
        //提取日期时间
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_11.string(),
            """(\d{4}-\d{1,2}-\d{1,2} \d{1,2}:\d{1,2}:\d{1,2})"""
        ),
        //提取QQ号码
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_12.string(),
            """([1-9][0-9]{4,})"""
        ),
        //提取微信号
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_regex_13.string(),
            """([a-zA-Z][a-zA-Z0-9_-]{5,19})"""
        ),
    )

    val AIPromptActions = listOf(
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_8.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_8_prompt.string()
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_1.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_1_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_2.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_2_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_3.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_3_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_4.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_4_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_5.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_5_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_6.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_6_prompt.string(),
        ),
        ScriptSpanParamLayout.QuickAction(
            com.hive.i8n.R.string.sc_quick_action_ai_7.string(),
            com.hive.i8n.R.string.sc_quick_action_ai_7_prompt.string(),
        ),
    )

    /**
     * 非中文屏幕适配
     */
    fun compatWideScreen(): Boolean {
        LanguageManager.getLanguage(GlobalApp.getContext()).let {
            return !it.lowercase().contains("zh")
        }
    }


    /**
     * 获取下载目录。
     * 有公共下载目录权限时返回 /storage/emulated/0/Download/；
     * 无权限时返回应用私有目录下的 download 子目录（无需权限）。
     */
    fun getDownloadPath(): String {
        val hasPermission = ScriptPermissionManager.checkMissedPermissions(
            listOf(ScriptHelper.PERMISSION_DOWNLOAD)
        ).isEmpty()
        return if (hasPermission) {
            "/storage/emulated/0/Download/"
        } else {
            BaseConst.getBaseDownloadDir()
        }
    }
}
