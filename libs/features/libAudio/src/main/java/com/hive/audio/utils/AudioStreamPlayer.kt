package com.hive.audio.utils

import com.hive.audio.providers.ms.MSAudioTtsEngine
import com.hive.utils.thread.UIHandlerUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AudioStreamPlayer(
    private val engine: MSAudioTtsEngine
) {
    private val executionQueue = BlockingExecutionQueue()

    fun enqueueText(
        text: String,
        onStart: ((text: String) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        engine.preCacheAudio(text)
        executionQueue.addTask {
            UIHandlerUtils.getInstance().executeInMainThread {
                onStart?.invoke(text)
            }

            playAudioSync(text)
            UIHandlerUtils.getInstance().executeInMainThread {
                onFinished?.invoke()
            }
        }
    }

    private fun playAudioSync(text: String) {
        engine.start(text)
    }

    fun release() {
        executionQueue.clearTask()
        engine.release()
    }


    class BlockingExecutionQueue {
        private val queue = mutableListOf<suspend () -> Unit>()
        private var isExecuting = false

        fun addTask(task: suspend () -> Unit) {
            queue.add(task)
            if (!isExecuting) {
                executeNextTask()
            }
        }

        private fun executeNextTask() {
            if (queue.isNotEmpty()) {
                val task = queue.removeAt(0)
                isExecuting = true
                GlobalScope.launch {
                    task.invoke()
                    isExecuting = false
                    executeNextTask()
                }
            }
        }

        fun clearTask() {
            queue.clear()
        }
    }


}

