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

package com.tencent.devops.process.api.codecc

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.transfer.TransferRequest
import com.tencent.devops.process.service.codecc.CodeccTransferService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCodeccTransferResourceImpl @Autowired constructor(
    private val codeccTransferService: CodeccTransferService
) : ServiceCodeccTransferResource {
    override fun transferToV2(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        return Result(codeccTransferService.transferToV2(projectId, pipelineIds))
    }

    override fun transferToV3(pipelineIds: Set<String>): Result<Map<String, String>> {
        return Result(codeccTransferService.transferToV3(pipelineIds))
    }

    override fun addToolSetToPipeline(
        projectId: String,
        toolRuleSet: String,
        language: ProjectLanguage,
        pipelineIds: Set<String>?
    ): Result<Map<String, String>> {
        return Result(codeccTransferService.addToolSetToPipeline(projectId, pipelineIds, toolRuleSet, language))
    }

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
        return Result(codeccTransferService.getHistoryBuildScan(
            status,
            trigger,
            queueTimeStartTime,
            queueTimeEndTime,
            startTimeStartTime,
            startTimeEndTime,
            endTimeStartTime,
            endTimeEndTime
        ))
    }

    override fun transferToV3Common(transferRequest: TransferRequest): Result<Map<String, String>> {
        return Result(codeccTransferService.transferToV3Common(transferRequest))
    }
}