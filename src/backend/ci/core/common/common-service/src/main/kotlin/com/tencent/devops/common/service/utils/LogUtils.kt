/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.service.utils

import com.tencent.devops.common.api.util.Watcher
import org.slf4j.LoggerFactory

object LogUtils {

    private val LOG = LoggerFactory.getLogger(LogUtils::class.java)

    fun costTime(message: String, startTime: Long, warnThreshold: Long = 1000, errorThreshold: Long = 5000) {
        val cost = System.currentTimeMillis() - startTime
        when {
            cost < warnThreshold -> {
                LOG.info("$message cost $cost ms")
            }
            cost in warnThreshold until errorThreshold -> {
                LOG.warn("$message cost $cost ms")
            }
            else -> {
                LOG.error("$message cost $cost ms")
            }
        }
    }

    /**
     * 计算[watcher].createTime与当前时间的毫秒数的耗时在[warnThreshold]与[errorThreshold]之间，
     * 会将[watcher]序列化为字符串并打印到WARN日志，当超出[errorThreshold]会打印ERROR日志。否则什么都不会打印
     */
    fun printCostTimeWE(watcher: Watcher, warnThreshold: Long = 1000, errorThreshold: Long = 5000) {
        watcher.stop()
        val cost = System.currentTimeMillis() - watcher.createTime
        if (cost >= warnThreshold) {
            if (cost > errorThreshold) {
                LOG.error("$watcher cost $cost ms")
            } else {
                LOG.warn("$watcher cost $cost ms")
            }
        }
    }

    /**
     * 获取有限长度的日志内容，默认最大长度为16K
     * @param logStr 原始日志内容
     * @return 截取后的日志
     */
    fun getLogWithLengthLimit(logStr: String?): String? {
        val defaultMaxLength = 16384
        return getLogWithLengthLimit(logStr, defaultMaxLength)
    }

    /**
     * 获取有限长度的日志内容
     * @param logStr 原始日志内容
     * @param maxLength 最大长度，若小于0则不生效，返回原始日志
     * @return 截取后的日志
     */
    fun getLogWithLengthLimit(logStr: String?, maxLength: Int): String? {
        if (logStr == null) {
            return null
        }
        if (maxLength < 0) {
            return logStr
        }
        return if (logStr.length > maxLength)
            logStr.substring(0, maxLength)
        else
            logStr
    }
}
