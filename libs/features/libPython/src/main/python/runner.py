"""
Chaquopy 执行器：执行完整 Python 脚本并实时捕获输出
脚本由 Kotlin PythonProvider.buildPythonScript 构建，已包含输入变量注入和输出收集逻辑
支持实时输出回调到 Kotlin 监听器
支持从 Kotlin 端中断执行（通过 executionControl.shouldStop()）
"""
import sys
import builtins
from io import StringIO


def run_script(script, output_listener=None, execution_control=None):
    """
    执行完整 Python 脚本，支持实时输出和中断控制
    :param script: 由 buildPythonScript 构建的完整脚本
    :param output_listener: Kotlin 输出监听器对象（可选）
    :param execution_control: Kotlin 执行控制对象（可选，用于中断检查）
    :return: (exit_code, output, error) 元组
    """
    old_stdout = sys.stdout
    old_stderr = sys.stderr

    # 实时输出流（带回调）
    out_stream = RealtimeOutputStream(old_stdout, output_listener, is_stderr=False)
    err_stream = RealtimeOutputStream(old_stderr, output_listener, is_stderr=True)

    # 创建统一的命名空间（避免 exec 作用域问题）
    exec_namespace = {'__builtins__': builtins.__dict__}

    # 注入中断检查函数到全局作用域
    if execution_control:
        exec_namespace['check_interrupt'] = lambda: check_interrupt(execution_control)

    try:
        sys.stdout = out_stream
        sys.stderr = err_stream

        # 使用显式 globals/locals 执行脚本，确保变量作用域一致
        exec(script, exec_namespace, exec_namespace)
        exit_code = 0
    except ScriptInterruptException:
        # 脚本被中断，正常退出
        err_stream.write("[Script interrupted by user]\n")
        exit_code = 0  # 中断视为正常退出（非错误）
    except Exception as e:
        err_stream.write(str(e))
        exit_code = 1
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr

    # 返回累计的输出内容（用于最终结果）
    output = out_stream.getvalue().strip()
    error = err_stream.getvalue().strip()

    return exit_code, output, error


def check_interrupt(execution_control):
    """
    检查是否应该停止执行（由 Kotlin 调用）
    如果 Kotlin 设置了停止标志，则抛出 ScriptInterruptException
    """
    if execution_control and execution_control.shouldStop():
        raise ScriptInterruptException()


class ScriptInterruptException(Exception):
    """
    脚本中断异常（用户主动停止）
    """
    pass


class RealtimeOutputStream:
    """
    实时输出流：缓冲输出内容，同时实时回调到 Kotlin 监听器
    """
    def __init__(self, original_stream, listener, is_stderr=False):
        self.buffer = StringIO()
        self.original_stream = original_stream  # 原始输出流（用于本地调试）
        self.listener = listener  # Kotlin 监听器对象
        self.is_stderr = is_stderr

    def write(self, text):
        # 缓存到内部 buffer（用于最终返回）
        self.buffer.write(text)

        # 写入原始流（本地调试可见）
        if self.original_stream:
            self.original_stream.write(text)

        # 回调 Kotlin 监听器（实时传递）
        if self.listener:
            try:
                if self.is_stderr:
                    self.listener.onStderr(text)
                else:
                    self.listener.onStdout(text)
            except Exception as e:
                # 回调失败不影响执行，仅记录
                if self.original_stream:
                    self.original_stream.write(f"[Callback Error: {e}]\n")

    def getvalue(self):
        return self.buffer.getvalue()

    def flush(self):
        # 刷新原始流
        if self.original_stream:
            self.original_stream.flush()
