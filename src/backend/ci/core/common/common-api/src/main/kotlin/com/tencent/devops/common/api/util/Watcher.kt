package com.tencent.devops.common.api.util

import org.springframework.util.StopWatch

/**
 * 非线程安全
 * 针对running值导致抛出异常的位置主动做了stop，并增加了一个全部耗时统计
 */
class Watcher(id: String = "") : StopWatch(id) {

    private var createTime: Long = System.currentTimeMillis()

    fun elapsed() = System.currentTimeMillis() - createTime

    override fun start() {
        if (isRunning) {
            stop()
        }
        super.start()
    }

    override fun start(taskName: String?) {
        if (isRunning) {
            stop()
        }
        super.start(taskName)
    }

    override fun toString(): String {
        if (isRunning) {
            stop()
        }
        return super.toString()
    }

    override fun shortSummary(): String {
        return super.shortSummary() + ", elapsed=${elapsed()}ms"
    }

    override fun stop() {
        if (isRunning) {
            super.stop()
        }
    }
}