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

package com.tencent.devops.statistics.service.process.codecc

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.statistics.dao.process.TencentPipelineBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CodeccTransferService @Autowired constructor(
    private val tencentPipelineBuildDao: TencentPipelineBuildDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeccTransferService::class.java)
    }

    fun getHistoryBuildScan(
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?
    ): List<BuildBasicInfo> {
        var queueTimeStartTimeTemp = queueTimeStartTime
        val dayTimeMillis = 24 * 60 * 60 * 1000
        if (queueTimeStartTime != null && queueTimeStartTime > 0 && queueTimeEndTime != null && queueTimeEndTime > 0) {
            if (queueTimeEndTime - queueTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                queueTimeStartTimeTemp = queueTimeEndTime - dayTimeMillis
            }
        }

        var startTimeStartTimeTemp = startTimeStartTime
        if (startTimeStartTime != null && startTimeStartTime > 0 && startTimeEndTime != null && startTimeEndTime > 0) {
            if (startTimeEndTime - startTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                startTimeStartTimeTemp = startTimeEndTime - dayTimeMillis
            }
        }

        var endTimeStartTimeTemp = endTimeStartTime
        if (endTimeStartTime != null && endTimeStartTime > 0 && endTimeEndTime != null && endTimeEndTime > 0) {
            if (endTimeEndTime - endTimeStartTime > dayTimeMillis) { // 做下保护，不超过一天
                endTimeStartTimeTemp = endTimeEndTime - dayTimeMillis
            }
        }

        val list = tencentPipelineBuildDao.listScanPipelineBuildList(
            dslContext,
            status,
            trigger,
            queueTimeStartTimeTemp,
            queueTimeEndTime,
            startTimeStartTimeTemp,
            startTimeEndTime,
            endTimeStartTimeTemp,
            endTimeEndTime
        )
        val result = mutableListOf<BuildBasicInfo>()
        val buildIds = mutableSetOf<String>()
        list.forEach {
            val buildId = it.buildId
            if (buildIds.contains(buildId)) {
                return@forEach
            }
            buildIds.add(buildId)
            result.add(genBuildBaseInfo(it))
        }
        return result
    }

    private fun genBuildBaseInfo(
        tPipelineBuildHistoryRecord: TPipelineBuildHistoryRecord
    ): BuildBasicInfo {
        return with(tPipelineBuildHistoryRecord) {
            BuildBasicInfo(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = version
            )
        }
    }
}
