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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.pojo.VersionLog
import com.tencent.devops.store.pojo.vo.VersionLogVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExtServiceVersionLogService @Autowired constructor() {
    @Autowired
    lateinit var extServiceVersionDao: ExtServiceVersionLogDao
    @Autowired
    lateinit var dslContext: DSLContext

    fun listVersionLog(
        serviceId: String
    ): Result<VersionLogVO?> {
        val logRecords = extServiceVersionDao.listVersionLogByServiceId(dslContext, serviceId)
        val count = extServiceVersionDao.countVersionLogByServiceId(dslContext, serviceId)
        val logList = mutableListOf<VersionLog>()
        if (logRecords != null) {
            for (logRecord in logRecords) {
                logList.add(
                    VersionLog(
                        logId = logRecord.id,
                        serviceId = logRecord.serviceId,
                        releaseType = logRecord.releaseType.toString(),
                        content = logRecord.content,
                        createTime = DateTimeUtil.toDateTime(logRecord.createTime as LocalDateTime),
                        createUser = logRecord.creator
                    )
                )
            }
        }

        val result = VersionLogVO(count, logList)

        return Result(result)
    }

    fun getVersionLog(
        logId: String
    ): Result<VersionLog> {
        val logRecord = extServiceVersionDao.getVersionLogById(
            dslContext, logId
        )
        val result = VersionLog(
            logId = logRecord.id,
            serviceId = logRecord.serviceId,
            releaseType = logRecord.releaseType.toString(),
            content = logRecord.content,
            createTime = DateTimeUtil.toDateTime(logRecord.createTime as LocalDateTime),
            createUser = logRecord.creator
        )
        return Result(result)
    }
}
