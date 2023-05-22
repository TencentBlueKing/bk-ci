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

package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.plugin.codecc.pojo.BlueShieldResponse
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.plugin.codecc.pojo.CodeccMeasureInfo
import com.tencent.devops.plugin.codecc.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService,
    private val codeccApi: CodeccApi
) : ServiceCodeccResource {

    override fun callback(callback: CodeccCallback): Result<String> {
        return Result(codeccService.callback(callback))
    }

    override fun getCodeccBuildInfo(buildIds: Set<String>): Result<Map<String, CodeccBuildInfo>> {
        return Result(codeccService.getCodeccBuildInfo(buildIds))
    }

    override fun getCodeccTaskByProject(
        beginDate: Long?,
        endDate: Long?,
        projectIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>> {
        return Result(codeccService.getCodeccTaskByProject(beginDate, endDate, projectIds))
    }

    override fun getCodeccTaskByPipeline(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>> {
        return Result(codeccService.getCodeccTaskByPipeline(beginDate, endDate, pipelineIds))
    }

    override fun getCodeccTaskResult(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, CodeccCallback>> {
        return Result(codeccService.getCodeccTaskResult(beginDate, endDate, pipelineIds))
    }

    override fun getCodeccTaskResult(buildIds: Set<String>): Result<Map<String, CodeccCallback>> {
        return Result(codeccService.getCodeccTaskResultByBuildIds(buildIds))
    }

    override fun installCheckerSet(
        projectId: String,
        userId: String,
        type: String,
        checkerSetId: String
    ): Result<Boolean> {
        return codeccApi.installCheckerSet(projectId, userId, type, checkerSetId)
    }

    override fun getCodeccMeasureInfo(repoId: String, buildId: String?): Result<CodeccMeasureInfo?> {
        return codeccApi.getCodeccMeasureInfo(repoId, buildId)
    }

    override fun getCodeccTaskStatusInfo(repoId: String, buildId: String?): Result<Int> {
        return codeccApi.getCodeccTaskStatusInfo(repoId, buildId)
    }

    override fun startCodeccTask(repoId: String, commitId: String?): Result<String> {
        return codeccApi.startCodeccTask(repoId, commitId)
    }

    override fun createCodeccPipeline(repoId: String, languages: List<String>): Result<Boolean> {
        return codeccApi.createCodeccPipeline(repoId, languages)
    }

    override fun getCodeccOpensourceMeasurement(codeSrc: String): Result<Map<String, Any>> {
        return codeccApi.getCodeccOpensourceMeasurement(codeSrc)
    }
}
