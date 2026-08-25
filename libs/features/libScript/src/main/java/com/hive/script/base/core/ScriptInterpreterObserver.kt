// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import com.hive.script.base.ScriptCommand
import com.hive.script.views.logger.ScriptLoggerView

/**
 *
 * @author jiadou
 * @date 7/9/21
 */
class ScriptInterpreterObserver {

    interface LoggerObserver {

        fun onLogger(script: ScriptCommand?, type: ScriptLoggerView.LogType, msg: String?) {}

    }

    interface CommandRecordObserver {

        fun onCommandRecordAdded(script: ScriptCommand) {}

        fun onCommandRecordRemoved(script: ScriptCommand) {}

        fun onCommandRecordChanged() {}

    }

    interface InterpreterExecuteObserver {
        fun onInterpreterStart(cmd: ScriptCommand) {}

        fun onInterpreterEnd(cmd: ScriptCommand) {}

        fun onInterpreterTryStop(cmd: ScriptCommand) {}
    }

    interface CommandExecuteObserver {
        fun onCommandExecuteBefore(cmd: ScriptCommand) {}

        fun onCommandExecuteEvent(type: Int, cmd: ScriptCommand, obj: Any?) {}

        fun onCommandExecuteWait(cmd: ScriptCommand, delay: Long) {}

        fun onCommandExecuteAfter(cmd: ScriptCommand) {}
    }

    companion object {

        @JvmStatic
        val interpreterSubscriber: MutableList<InterpreterExecuteObserver> by lazy {
            mutableListOf()
        }

        @JvmStatic
        val commandSubscriber: MutableList<CommandExecuteObserver> by lazy {
            mutableListOf()
        }

        @JvmStatic
        val commandRecordSubscriber: MutableList<CommandRecordObserver> by lazy {
            mutableListOf()
        }

        @JvmStatic
        val commandLoggerSubscriber: MutableList<LoggerObserver> by lazy {
            mutableListOf()
        }

        @JvmStatic
        fun registerLoggerObserver(observer: LoggerObserver) {
            if (!commandLoggerSubscriber.contains(observer)) commandLoggerSubscriber.add(observer)
        }

        @JvmStatic
        fun unRegisterLoggerObserver(observer: LoggerObserver) {
            if (commandLoggerSubscriber.contains(observer)) commandLoggerSubscriber.remove(observer)
        }

        @JvmStatic
        fun registerCommandRecordObserver(observer: CommandRecordObserver) {
            if (!commandRecordSubscriber.contains(observer)) commandRecordSubscriber.add(observer)
        }

        @JvmStatic
        fun registerCommandObserver(observer: CommandExecuteObserver) {
            if (!commandSubscriber.contains(observer)) commandSubscriber.add(observer)
        }

        @JvmStatic
        fun registerInterpreterObserver(observer: InterpreterExecuteObserver) {
            if (!interpreterSubscriber.contains(observer)) interpreterSubscriber.add(observer)
        }

        @JvmStatic
        fun unRegisterCommandObserver(observer: CommandExecuteObserver) {
            if (commandSubscriber.contains(observer)) commandSubscriber.remove(observer)
        }

        @JvmStatic
        fun unRegisterInterpreterObserver(observer: InterpreterExecuteObserver) {
            if (interpreterSubscriber.contains(observer)) interpreterSubscriber.remove(observer)
        }

        @JvmStatic
        fun unRegisterCommandRecordObserver(observer: CommandRecordObserver) {
            if (commandRecordSubscriber.contains(observer)) commandRecordSubscriber.remove(observer)
        }

        @JvmStatic
        fun notifyInterpreterStart(cmd: ScriptCommand) {
            interpreterSubscriber.forEach {
                try {
                    it.onInterpreterStart(cmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyInterpreterEnd(cmd: ScriptCommand) {
            interpreterSubscriber.forEach {
                try {
                    it.onInterpreterEnd(cmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyInterpreterTryStop(cmd: ScriptCommand) {
            interpreterSubscriber.forEach {
                try {
                    it.onInterpreterTryStop(cmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandExecuteBefore(cmd: ScriptCommand) {
            commandSubscriber.forEach {
                try {
                    it.onCommandExecuteBefore(cmd)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandExecuteWait(cmd: ScriptCommand, delay: Long) {
            commandSubscriber.forEach {
                try {
                    it.onCommandExecuteWait(cmd, delay)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandExecuteEvent(type: Int, cmd: ScriptCommand, obj: Any? = null) {
            commandSubscriber.forEach {
                try {
                    it.onCommandExecuteEvent(type, cmd, obj)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandExecuteAfter(cmd: ScriptCommand) {
            commandSubscriber.forEach {
                try {
                    it.onCommandExecuteAfter(cmd)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandRecordAdded(script: ScriptCommand) {
            commandRecordSubscriber.forEach {
                try {
                    it.onCommandRecordAdded(script)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        @JvmStatic
        fun notifyCommandRecordRemoved(script: ScriptCommand) {
            commandRecordSubscriber.forEach {
                try {
                    it.onCommandRecordRemoved(script)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyCommandRecordChanged() {
            commandRecordSubscriber.forEach {
                try {
                    it.onCommandRecordChanged()
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun notifyLogger(script: ScriptCommand?, type: ScriptLoggerView.LogType, msg: String?) {
            commandLoggerSubscriber.forEach {
                try {
                    it.onLogger(script, type, msg)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}