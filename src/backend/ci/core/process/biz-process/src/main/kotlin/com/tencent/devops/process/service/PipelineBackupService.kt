package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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

@Service
class PipelineBackupService @Autowired constructor(
    val redisOperation: RedisOperation
) {

    @Value("\${emoji.db.backup}")
    private val backUpTag: String = ""

    val detailLabel = "DETAIL"
    val startupLabel = "STARTUP"
    val resourceLabel = "RESOURCE"
    val historyLabel = "HISTORY"

    fun isBackUp(type: String): Boolean {
        if (backUpTag.isNullOrEmpty() || !backUpTag.toBoolean()) {
            return false
        }

        val backupTable = redisOperation.get(REDIS_KEY)

        if (backupTable.isNullOrEmpty()) {
            return false
        }

        if (type == backupTable) {
            return true
        }

        return false
    }


    fun getBackupTag(): String {
        return backUpTag + redisOperation.get(REDIS_KEY)
    }

    companion object {
        const val REDIS_KEY = "pipeline:backup:table:"
    }
}
