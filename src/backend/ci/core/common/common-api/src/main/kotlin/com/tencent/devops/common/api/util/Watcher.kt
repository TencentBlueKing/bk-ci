/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch

/**
 * 非线程安全
 * 针对running值导致抛出异常的位置主动做了stop，并增加了一个全部耗时统计
 */
class Watcher(id: String = "") : StopWatch(id) {

    val createTime: Long = System.currentTimeMillis()

    fun elapsed() = System.currentTimeMillis() - createTime

    override fun start() {
        if (isRunning) {
            stop()
        }
        super.start()
    }

    override fun start(taskName: String) {
        if (isRunning) {
            stop()
        }
        super.start(taskName)
    }

    override fun toString(): String {
        if (isRunning) {
            stop()
        }
        val sb = StringBuilder(shortSummary())
        this.taskInfo.forEach { task ->
            sb.append("|").append(task.taskName).append("=").append(task.timeMillis)
        }
        return sb.toString()
    }

    override fun shortSummary(): String {
        return "watcher|$id|total=$totalTimeMillis|elapsed=${elapsed()}"
    }

    override fun stop() {
        if (isRunning) {
            try {
                super.stop()
            } catch (ignored: IllegalStateException) {
            }
        }
    }

    /**
     * 监听action耗时 , 会忽略异常
     */
    fun safeAround(taskName: String, action: () -> Unit) {
        try {
            this.start(taskName)
            action()
        } catch (e: Exception) {
            logger.warn("$id , $taskName", e)
        } finally {
            this.stop()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Watcher::class.java)
    }
}
