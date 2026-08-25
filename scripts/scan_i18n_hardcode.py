#!/usr/bin/env python3
"""
国际化硬编码扫描工具
扫描 Kotlin/Java 源码中的未国际化硬编码字符串，输出报告供人工排查。

检测规则：
1. 含中文的字符串字面量（高优先级）
2. 常见用户界面 API 传入的硬编码字符串：showToast、setText、setContentTitle 等
3. 消息/内容字段赋值：content = "xxx", toolCallResult = "xxx" 等

排除规则（report 仅存需国际化的结果，其余不写入）：
- Log/DLog/println 等调试输出
- 注释内的字符串（含 // TODO、// FIXME、// 临时）
- 已使用 R.string 或 com.hive.i8n.R.string 的调用
- 空串、纯空格、单字符
- URL、正则、JSON key、格式占位符等技术字符串
- 测试代码
- 服务端配置（BuildConfigHelper、API 端点等）
- 工程师/调试（EngineerHelper、EngineerActivity、AgentDebugFragment、SimpleDebugger）
- 崩溃界面（CrashDetailActivity、CrashHandler）
- 语言设置页面（ActivityLanguages）
- 日期/时间组件（wheel、TimePicker、DatePicker、ScheduleTimer、CalendarUtils 等）
- AI Provider 内部日志、异常消息
- 系统/API 常量（getIdentifier、ObjectAnimator、putExtra 等）

用法：python3 scripts/scan_i18n_hardcode.py [--output report.txt] [--verbose]
"""

import argparse
import os
import re
import sys
from pathlib import Path


# 扫描的源码目录（相对于项目根目录）
SOURCE_DIRS = [
    "appScript/src/main/java",
    "baseApp/src/main/java",
    "libs/features/libAgent/src/main/java",
    "libs/features/libScript/src/main/java",
    "libs/features/libEditor/src/main/java",
    "libs/features/libFiles/src/main/java",
    "libs/features/libImage/src/main/java",
    "libs/features/libMCP/src/main/java",
    "libs/features/libOcr/src/main/java",
    "libs/features/libTimer/src/main/java",
    "libs/features/libOpenCV/src/main/java",
    "libs/framework/libBase/src/main/java",
    "libs/framework/libUtils/src/main/java",
    "libs/framework/libViews/src/main/java",
    "libs/framework/libNet/src/main/java",
    "libs/framework/libCompon/src/main/java",
]

# 排除的路径片段（命中则整个文件跳过）- report 仅存需国际化的结果
EXCLUDE_PATHS = [
    "/build/",
    "/test/",
    "/androidTest/",
    "/generated/",
    "BuildConfig",
    ".gradle",
    "/R.",
    # 广告统计/埋点
    # 崩溃/调试界面
    "CrashDetailActivity",
    "CrashHandler",
    # 工程师/调试工具
    "EngineerHelper",
    "EngineerActivity",
    "SimpleDebugger",
    "AgentDebugFragment",
    # 语言设置页面
    "ActivityLanguages",
    # 统计/埋点相关
    "Statistic",
    "Statistics",
    # 日期/时间选择器组件
    "/wheel/",
    "TimePicker",
    "DatePicker",
    "LayoutTimePicker",
    "LayoutTimer",
    "LayoutRegionPicker",
    "ScheduleTimer",
    "CalendarUtils",
    "ScreenLockDateHelper",
]

# 排除的整行上下文（命中则该行所有字符串跳过）
EXCLUDE_LINE_CONTEXTS = [
    r"@Headers\s*\(",           # HTTP 请求头，技术常量
    r"appendLine\s*\(",         # 注入的 Python 代码注释
    r"Log\.\w+\s*\(",           # 已在 LOG_PATTERNS，双重保险
    r"DLog\.\w+\s*\(",
    r"BuildConfigHelper\.getMap",
    r"BuildConfig\.",
    r"getIdentifier\s*\(",
    r"ObjectAnimator\.of(Float|Int)\s*\(",
    r"putExtra\s*\(",
    r"getSerializableExtra\s*\(",
    r"getStringExtra\s*\(",
    r"hasMarked\s*\(",
    r"\.mark\s*\(",
    r"\.unmark\s*\(",
    r"EngineerHelper\.register(Switcher|TestEvent)",
    r"//\s*(TODO|FIXME|临时|TEMP)",
]

# 按文件路径排除的上下文（仅在匹配路径的文件中，命中上下文则跳过）
EXCLUDE_FILE_CONTEXTS = {
    # AI Provider 内部：日志、异常、API 错误处理、缓存状态、内部消息
    "Provider": [
        r"throw\s+(Exception|IllegalArgumentException)\s*\(",
        r"DLog\.\w+\s*\(",
        r'\?\:\s*"[^"]*"',           # ?: "未知错误" 等 fallback
        r'errorStream.*readText.*\?\:',  # API 错误流 fallback
        r"(return\s+|message\.content\s*=|\.content\s*=)\s*\"",  # return "xxx" 缓存状态、content 内部 AI 上下文
        r'API请求失败',                # API 错误文案（内部）
        r'处理模型:',                   # 模型处理日志
        r'缓存中有',                    # 缓存状态
        r'无缓存',                      # 缓存状态
        r'工具执行的图片信息',          # 内部 AI 上下文
    ],
    # 语言选择器（已整文件排除，此处保留备用）
    "ActivityLanguages": [
        r'"[a-z]{2}[A-Z]{0,2}"\s+to\s+"[^"]+"',  # "zhCN" to "简体"
    ],
    # AI 协调器：任务状态、内部执行日志（notifyTaskInfoUpdated 多为调试状态）
    "AICoordinator": [
        r"AI 思考",                              # 任务状态
        r"执行工具:",                             # 内部执行日志
        r"执行结果:",                             # 任务状态
        r"执行失败:",                             # 工具执行结果（内部）
        r"执行 AI 助手工具:",                    # 任务状态
        r"准备执行工具调用:",                     # 任务状态
        r"选择 AI Provider:",                    # 内部日志
        r"未找到支持该方法的工具:",                # 内部异常
    ],
    # Agent 上下文、Provider：内部状态
    "AgentContext": [
        r"任务状态变化:",                         # 内部状态通知
    ],
    "XAgentProvider": [
        r"执行工具时发生异常:",                   # 内部异常
    ],
    # 注入的 Python 脚本（注释为用户不可见）
    "CmdPythonExecutor": [
        r"appendLine\s*\(",
    ],
    # 剪贴板粘贴关键词匹配（多语言列表，非 UI 文案）
    "ScriptTextInputHelper": [
        r"粘贴.*Paste",  # paste 关键词多语言列表
        r"文本设置失败",   # DLog 调试输出
    ],
    # 存储类型常量注释
    "StorageDetect": [
        r"//\s*\"",  # 注释内的字符串
    ],
}

# 已正确使用 i18n 的模式（不视为硬编码）
I18N_PATTERNS = [
    r"R\.string\.\w+",
    r"com\.hive\.i8n\.R\.string\.\w+",
    r"getString\s*\(\s*[^)]+R\.string",
    r"getString\s*\(\s*com\.hive\.i8n\.R\.string",
    r"\.string\s*\(\s*R\.string",  # Int.string(R.string.xxx)
]

# 调试/日志相关，排除
LOG_PATTERNS = [
    r"Log\.\w+\s*\(",
    r"DLog\.\w+\s*\(",
    r"println\s*\(",
    r"print\s*\(",
    r"e\.printStackTrace",
]

# 用户界面 API：这些方法的字符串参数应为国际化资源
UI_API_PATTERNS = [
    (r"\.showToast\s*\(\s*\"([^\"]+)\"", "showToast"),
    (r"CommonToast\.show\s*\(\s*\"([^\"]+)\"", "CommonToast.show"),
    (r"\.setText\s*\(\s*\"([^\"]+)\"", "setText"),
    (r"\.setContentTitle\s*\(\s*\"([^\"]+)\"", "setContentTitle"),
    (r"\.setContentText\s*\(\s*\"([^\"]+)\"", "setContentText"),
    (r"\.setTitle\s*\(\s*\"([^\"]+)\"", "setTitle"),
    (r"\.setMessage\s*\(\s*\"([^\"]+)\"", "setMessage"),
    (r"Toast\.makeText\s*\([^,]+,\s*\"([^\"]+)\"", "Toast.makeText"),
    (r"\.setPositiveButton\s*\(\s*\"([^\"]+)\"", "setPositiveButton"),
    (r"\.setNegativeButton\s*\(\s*\"([^\"]+)\"", "setNegativeButton"),
    (r"\.setNeutralButton\s*\(\s*\"([^\"]+)\"", "setNeutralButton"),
    (r"content\s*=\s*\"([^\"]+)\"", "content = "),  # 消息内容（Provider 内多为内部 AI 上下文，单独排除）
    (r"toolCallResult\s*=\s*\"([^\"]+)\"", "toolCallResult = "),
    (r"apiKeyValidateMsg\s*:\s*String\s*=\s*\"([^\"]+)\"", "apiKeyValidateMsg"),
    (r"throw\s+Exception\s*\(\s*\"([^\"]+)\"", "throw Exception"),  # 可能对用户展示
    (r"throw\s+IllegalArgumentException\s*\(\s*\"([^\"]+)\"", "throw IllegalArgumentException"),
]

# 中文匹配
CHINESE_RE = re.compile(r'[\u4e00-\u9fff]+')

# 字符串字面量：匹配 "xxx" 或 """xxx""" (Kotlin raw string)
STRING_LITERAL_RE = re.compile(
    r'"(?:[^"\\]|\\.)*"',  # 双引号字符串，支持 \"
)

# 三引号字符串（Kotlin）
TRIPLE_QUOTE_RE = re.compile(
    r'"""(?:[^"]|"(?!""))*"""',
    re.DOTALL
)


def should_exclude_path(filepath: str) -> bool:
    for exc in EXCLUDE_PATHS:
        if exc in filepath:
            return True
    return False


def is_technical_string(s: str) -> bool:
    """判断是否为技术性字符串，通常不需国际化"""
    if not s or len(s.strip()) < 2:
        return True
    # URL、路径
    if s.startswith(("http://", "https://", "ws://", "wss://", "file://", "/", ".")):
        return True
    # 格式占位符为主
    if re.match(r"^[%\s\d\$\-\.]+$", s) or ("%s" in s and "%d" in s and len(s) < 20):
        return True
    # 纯数字、标识符
    if re.match(r"^[a-zA-Z0-9_\.\-]+$", s) and len(s) < 40:
        return True
    # JSON/XML key 风格
    if re.match(r"^[\"\'\{\}\[\]:,\s]+$", s):
        return True
    return False


def has_chinese(s: str) -> bool:
    return bool(CHINESE_RE.search(s))


def line_is_in_comment(line: str, in_block_comment: list) -> bool:
    """简化判断：是否在注释内。in_block_comment 为 [bool] 的 list 用作 mutable 引用"""
    stripped = line.strip()
    if "/*" in line:
        in_block_comment[0] = True
    if in_block_comment[0]:
        if "*/" in line:
            in_block_comment[0] = False
        return True
    if stripped.startswith("//"):
        return True
    return False


def line_uses_i18n(line: str) -> bool:
    for pat in I18N_PATTERNS:
        if re.search(pat, line):
            return True
    return False


def line_is_log(line: str) -> bool:
    for pat in LOG_PATTERNS:
        if re.search(pat, line):
            return True
    return False


def line_matches_exclude_context(line: str, rel_path: str, category: str = "") -> bool:
    """检查该行是否命中排除的上下文"""
    for pat in EXCLUDE_LINE_CONTEXTS:
        if re.search(pat, line):
            return True
    # 按文件路径的条件排除
    for path_key, patterns in EXCLUDE_FILE_CONTEXTS.items():
        if path_key in rel_path:
            for pat in patterns:
                if re.search(pat, line):
                    return True
    return False


def should_skip_ui_api_result(api_name: str, line: str, rel_path: str) -> bool:
    """UI_API 结果是否应跳过：Provider 内的 content/toolCallResult 多为内部 AI 上下文"""
    if api_name in ("content = ", "toolCallResult = ") and "Provider" in rel_path:
        return True
    return False


def scan_file(filepath: Path, root: Path, verbose: bool) -> list:
    """
    扫描单个文件，返回 [(line_num, category, raw_string, context), ...]
    """
    results = []
    rel_path = str(filepath.relative_to(root))

    try:
        content = filepath.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        if verbose:
            print(f"  [skip] {rel_path}: {e}", file=sys.stderr)
        return []

    lines = content.splitlines()
    in_block_comment = [False]

    for i, line in enumerate(lines, 1):
        if line_is_in_comment(line, in_block_comment):
            continue

        # 1. 含中文的字符串字面量
        if not line_matches_exclude_context(line, rel_path, "CHINESE"):
            for m in STRING_LITERAL_RE.finditer(line):
                s = m.group(0)[1:-1].replace("\\\"", "\"").replace("\\\\", "\\")
                if has_chinese(s) and not is_technical_string(s):
                    if line_is_log(line):
                        continue
                    if line_uses_i18n(line) and m.group(0) not in line:
                        pass
                    results.append((i, "CHINESE", s[:80], line.strip()[:120]))

        # 2. UI API 硬编码
        for pat, api_name in UI_API_PATTERNS:
            for m in re.finditer(pat, line):
                s = m.group(1)
                if not s or is_technical_string(s):
                    continue
                if line_uses_i18n(line):
                    continue
                if line_matches_exclude_context(line, rel_path):
                    continue
                if should_skip_ui_api_result(api_name, line, rel_path):
                    continue
                results.append((i, f"UI_API:{api_name}", s[:80], line.strip()[:120]))

        # 3. 不含中文但较长的英文硬编码（可能用户可见，保守检测）
        if has_chinese(line):
            continue
        if not line_matches_exclude_context(line, rel_path, "ENGLISH_LONG"):
            for m in STRING_LITERAL_RE.finditer(line):
                s = m.group(0)[1:-1]
                if len(s) < 15 or is_technical_string(s):
                    continue
                if line_is_log(line) or line_uses_i18n(line):
                    continue
                # 排除常见技术字符串
                if s in ("image/*", "scaleX", "scaleY", "data", "mcp", "ai_assistant", "base"):
                    continue
                if re.match(r"^[a-zA-Z0-9_\.\-]+$", s):  # 纯标识符风格
                    continue
                if " " in s or s.endswith((".", "!", "?")):  # 像完整句子才报告
                    results.append((i, "ENGLISH_LONG", s[:80], line.strip()[:120]))

    return [(rel_path, r) for r in results]


def main():
    parser = argparse.ArgumentParser(
        description="扫描未国际化的硬编码字符串",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument("--output", "-o", help="输出报告到文件（默认 stdout）")
    parser.add_argument("--verbose", "-v", action="store_true", help="打印扫描进度")
    parser.add_argument("--root", "-r", default=".", help="项目根目录（默认当前目录）")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    if not root.exists():
        print(f"错误：根目录不存在 {root}", file=sys.stderr)
        sys.exit(1)

    all_results = []

    for src_dir in SOURCE_DIRS:
        full_dir = root / src_dir
        if not full_dir.exists():
            if args.verbose:
                print(f"  [skip dir] {src_dir}", file=sys.stderr)
            continue

        for ext in ("*.kt", "*.java"):
            for f in full_dir.rglob(ext):
                if should_exclude_path(str(f)):
                    continue
                if not f.is_file():
                    continue
                file_results = scan_file(f, root, args.verbose)
                for rel_path, (line_num, cat, s, ctx) in file_results:
                    all_results.append((rel_path, line_num, cat, s, ctx))

    # 去重（同一行可能匹配多种规则）
    seen = set()
    unique = []
    for r in all_results:
        key = (r[0], r[1], r[3])  # file, line, string
        if key not in seen:
            seen.add(key)
            unique.append(r)

    # 按文件、行号排序
    unique.sort(key=lambda x: (x[0], x[1]))

    out = open(args.output, "w", encoding="utf-8") if args.output else sys.stdout
    try:
        out.write("# 未国际化硬编码扫描报告\n")
        out.write(f"# 共发现 {len(unique)} 处待检查项\n\n")

        # 按类别分组输出
        by_cat = {}
        for rel_path, line_num, cat, s, ctx in unique:
            by_cat.setdefault(cat, []).append((rel_path, line_num, s, ctx))

        for cat in ("CHINESE", "UI_API:showToast", "UI_API:CommonToast.show", "UI_API:content = ", "UI_API:toolCallResult = ",
                    "UI_API:setText", "UI_API:setContentTitle", "UI_API:setContentText", "UI_API:throw Exception",
                    "UI_API:apiKeyValidateMsg", "ENGLISH_LONG"):
            items = by_cat.get(cat, [])
            if not items:
                continue
            out.write(f"\n## {cat} ({len(items)} 处)\n")
            out.write("-" * 60 + "\n")
            for rel_path, line_num, s, ctx in items:
                out.write(f"  {rel_path}:{line_num}\n")
                out.write(f"    字符串: {repr(s)}\n")
                out.write(f"    上下文: {ctx}\n\n")

        # 其余类别
        other_cats = set(by_cat.keys()) - {
            "CHINESE", "UI_API:showToast", "UI_API:CommonToast.show", "UI_API:content = ",
            "UI_API:toolCallResult = ", "UI_API:setText", "UI_API:setContentTitle", "UI_API:setContentText",
            "UI_API:throw Exception", "UI_API:apiKeyValidateMsg", "ENGLISH_LONG"
        }
        for cat in sorted(other_cats):
            items = by_cat.get(cat, [])
            out.write(f"\n## {cat} ({len(items)} 处)\n")
            out.write("-" * 60 + "\n")
            for rel_path, line_num, s, ctx in items:
                out.write(f"  {rel_path}:{line_num}\n")
                out.write(f"    字符串: {repr(s)}\n")
                out.write(f"    上下文: {ctx}\n\n")
    finally:
        if args.output and out != sys.stdout:
            out.close()

    if not args.output:
        print(f"\n扫描完成，共 {len(unique)} 处待检查。可用 -o report.txt 输出到文件。", file=sys.stderr)
    else:
        print(f"报告已写入 {args.output}，共 {len(unique)} 处。", file=sys.stderr)


if __name__ == "__main__":
    main()
