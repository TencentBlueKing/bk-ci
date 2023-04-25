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
package com.tencent.devops.statistics.openapi.resource.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.statistics.openapi.api.apigw.ApigwBuildResource
import com.tencent.devops.statistics.service.process.codecc.CodeccTransferService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceImpl @Autowired constructor(
    private val codeccTransferService: CodeccTransferService
) : ApigwBuildResource {

    override fun getHistoryBuildScan(
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?
    ): Result<List<BuildBasicInfo>> {
        logger.info("Get the build by status(${status?.joinToString(",")}) " +
            "and trigger(${trigger?.joinToString(",")}) " +
            "and queueTimeStartTime($queueTimeStartTime)" +
            "and queueTimeEndTime($queueTimeEndTime)" +
            "and startTimeStartTime($startTimeStartTime)" +
            "and startTimeEndTime($startTimeEndTime)" +
            "and endTimeStartTime($endTimeStartTime)" +
            "and endTimeEndTime($endTimeEndTime")
        return Result(
            codeccTransferService.getHistoryBuildScan(
                status = status,
                trigger = trigger,
                queueTimeStartTime = queueTimeStartTime,
                queueTimeEndTime = queueTimeEndTime,
                startTimeStartTime = startTimeStartTime,
                startTimeEndTime = startTimeEndTime,
                endTimeStartTime = endTimeStartTime,
                endTimeEndTime = endTimeEndTime
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceImpl::class.java)
    }
}
