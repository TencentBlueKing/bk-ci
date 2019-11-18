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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.util

import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

object DateTimeUtils {

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    private val logger = LoggerFactory.getLogger(DateTimeUtils::class.java)

    /**
     * 单位转换，分钟转换秒
     */
    fun minuteToSecond(minutes: Int): Int {
        return minutes * 60
    }

    /**
     * 单位转化，秒转换分钟
     * 以Int为计算单位，有余数将省去
     */
    fun secondToMinute(seconds: Int): Int {
        return seconds / 60
    }

    /**
     * 单位转化，秒转换时间戳
     * 2019-09-02T08:58:46+0000 -> xxxxx
     */
    fun zoneDateToTimestamp(timeStr: String?): Long {
        try {
            if (timeStr.isNullOrBlank()) return 0L
            return formatter.parse(timeStr).time
        } catch (e: Exception) {
            logger.error("fail to parse time string: $timeStr", e)
        }
        return 0L
    }
}