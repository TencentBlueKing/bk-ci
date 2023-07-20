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

package com.tencent.devops.log.service

import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.log.dao.LogStatusDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("LongParameterList")
@Service
class LogStatusService @Autowired constructor(
    private val dslContext: DSLContext,
    private val logStatusDao: LogStatusDao
) {

    fun finish(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        logStorageMode: LogStorageMode?,
        finish: Boolean
    ) {
        JooqUtils.retryWhenDeadLock {
            logStatusDao.finish(
                dslContext = dslContext,
                buildId = buildId,
                // #8804 将db中保存字段兜底为空字符串，方便唯一键冲突判断
                tag = tag ?: "",
                subTags = subTag ?: "",
                jobId = jobId ?: "",
                executeCount = executeCount ?: 1,
                logStorageMode = logStorageMode ?: LogStorageMode.UPLOAD,
                finish = finish
            )
        }
    }

    fun updateStorageMode(
        buildId: String,
        executeCount: Int,
        propertyList: List<TaskBuildLogProperty>
    ) {
        logStatusDao.updateStorageMode(
            dslContext = dslContext,
            buildId = buildId,
            executeCount = executeCount,
            modeMap = propertyList.associate {
                it.elementId to it.logStorageMode
            }
        )
    }

    fun getStorageMode(
        buildId: String,
        tag: String,
        executeCount: Int
    ): QueryLogStatus {
        val record = logStatusDao.getStorageMode(
            dslContext = dslContext,
            buildId = buildId,
            tag = tag,
            executeCount = executeCount
        )
        return if (record != null) {
            QueryLogStatus(buildId, record.finished, LogStorageMode.parse(record.mode))
        } else {
            QueryLogStatus(buildId, false, LogStorageMode.UPLOAD)
        }
    }

    fun isFinish(
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ) = logStatusDao.isFinish(
        dslContext = dslContext,
        buildId = buildId,
        // #8804 将db中保存字段兜底为空字符串，方便唯一键冲突判断
        jobId = jobId,
        tag = tag ?: "",
        subTags = subTag ?: "",
        executeCount = executeCount ?: 1
    )
}
