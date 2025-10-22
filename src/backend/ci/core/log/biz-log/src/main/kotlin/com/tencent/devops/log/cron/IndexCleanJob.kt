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

package com.tencent.devops.log.cron

import com.tencent.devops.log.util.IndexNameUtils.LOG_INDEX_PREFIX
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("ReturnCount")
interface IndexCleanJob {

    fun cleanIndex()

    fun expire(deathLine: LocalDateTime, index: String): Boolean {
        try {
            if (!index.startsWith(LOG_INDEX_PREFIX)) {
                return false
            }
            val dateStr = index.replace(LOG_INDEX_PREFIX, "") + " 00:00"
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val date = LocalDateTime.parse(dateStr, format)

            if (deathLine > date) {
                logger.info("[$index] The index is expire ($deathLine|$date)")
                return true
            }
        } catch (ignore: Throwable) {
            logger.warn("[$index] Fail to check if the index expire", ignore)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexCleanJob::class.java)
    }
}
