// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.agent

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ExecutionContextStack : IExecutionContextStack {

    private val lock = ReentrantLock()
    private val frames = mutableListOf<ExecutionContextFrame>()
    private val observers = CopyOnWriteArrayList<IExecutionContextObserver>()

    override fun push(frame: ExecutionContextFrame) {
        val snapshot = lock.withLock {
            frames.add(frame)
            frames.toList()
        }
        notifyObservers(snapshot)
    }

    override fun pop(expectedId: String?): ExecutionContextFrame? {
        val resultAndSnapshot = lock.withLock {
            if (frames.isEmpty()) {
                return@withLock Pair(null, frames.toList())
            }
            val top = frames.last()
            if (expectedId != null && top.id != expectedId) {
                return@withLock Pair(null, frames.toList())
            }
            val removed = frames.removeAt(frames.lastIndex)
            Pair(removed, frames.toList())
        }
        notifyObservers(resultAndSnapshot.second)
        return resultAndSnapshot.first
    }

    override fun peek(): ExecutionContextFrame? = lock.withLock { frames.lastOrNull() }

    override fun snapshot(): List<ExecutionContextFrame> = lock.withLock { frames.toList() }

    override fun depth(): Int = lock.withLock { frames.size }

    override fun registerObserver(observer: IExecutionContextObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
            observer.onExecutionContextStackChanged(snapshot())
        }
    }

    override fun unregisterObserver(observer: IExecutionContextObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers(snapshot: List<ExecutionContextFrame>) {
        observers.forEach { observer ->
            try {
                observer.onExecutionContextStackChanged(snapshot)
            } catch (_: Throwable) {
                // ignore observer exceptions
            }
        }
    }
}

