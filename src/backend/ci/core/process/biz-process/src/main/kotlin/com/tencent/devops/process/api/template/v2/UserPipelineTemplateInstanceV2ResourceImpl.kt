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

package com.tencent.devops.process.api.template.v2

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceBase
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesRequest
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskDetail
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstancesTaskResult
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedResp
import com.tencent.devops.process.service.template.v2.PipelineTemplateInstanceService

@RestResource
class UserPipelineTemplateInstanceV2ResourceImpl(
    private val instanceFacadeService: PipelineTemplateInstanceService
) : UserPipelineTemplateInstanceV2Resource {
    override fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): TemplateOperationRet {
        return instanceFacadeService.createTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version,
            request = request
        )
    }

    override fun asyncCreateTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): Result<String> {
        return Result(
            data = instanceFacadeService.asyncCreateTemplateInstances(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version,
                request = request
            )
        )
    }

    override fun asyncUpdateTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): Result<String> {
        return Result(
            instanceFacadeService.asyncUpdateTemplateInstances(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version,
                request = request
            )
        )
    }

    override fun listTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        status: TemplatePipelineStatus?,
        templateVersion: Long?,
        repoHashId: String?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<PipelineTemplateRelatedResp>> {
        return Result(
            instanceFacadeService.list(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                pipelineName = pipelineName,
                updater = updater,
                status = status,
                templateVersion = templateVersion,
                repoHashId = repoHashId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineIds: Set<String>
    ): Result<Map<String, TemplateInstanceParams>> {
        return Result(
            instanceFacadeService.listTemplateInstancesParams(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                pipelineIds = pipelineIds
            )
        )
    }

    override fun getTemplateInstanceParamsById(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<TemplateInstanceParams> {
        return Result(
            instanceFacadeService.getTemplateInstanceParamsById(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        )
    }

    override fun getTemplateInstanceParamsByRef(
        userId: String,
        projectId: String,
        templateId: String,
        ref: String
    ): Result<TemplateInstanceParams> {
        return Result(
            instanceFacadeService.getTemplateInstanceParamsByRef(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                ref = ref
            )
        )
    }

    override fun preFetchTemplateInstance(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): Result<List<PrefetchReleaseResult>> {
        return Result(
            instanceFacadeService.preFetchTemplateInstance(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                request = request
            )
        )
    }

    override fun compare(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineId: String,
        comparedVersion: Long?
    ): Result<PipelineTemplateInstanceCompareResponse> {
        return Result(
            instanceFacadeService.compare(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                pipelineId = pipelineId,
                compareVersion = comparedVersion
            )
        )
    }

    override fun compareTemplateAndPipelineYaml(
        userId: String,
        projectId: String,
        templateId: String,
        templateVersion: Long,
        pipelineId: String,
        pipelineVersion: Int,
        useTemplateSettings: Boolean
    ): Result<PipelineTemplateInstanceCompareResponse> {
        return Result(
            instanceFacadeService.compareTemplateAndPipelineYaml(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                templateVersion = templateVersion,
                pipelineId = pipelineId,
                pipelineVersion = pipelineVersion,
                useTemplateSettings = useTemplateSettings
            )
        )
    }

    override fun getTemplateInstanceTaskResult(
        userId: String,
        projectId: String,
        baseId: String
    ): Result<PipelineTemplateInstancesTaskResult> {
        return Result(
            instanceFacadeService.getTemplateInstanceTaskResult(
                projectId = projectId,
                baseId = baseId
            )
        )
    }

    override fun listTemplateInstanceTask(
        userId: String,
        projectId: String,
        templateId: String,
        status: String?
    ): Result<List<PipelineTemplateInstanceBase>> {
        val statusList = status?.split(",") ?: listOf(
            TemplateInstanceStatus.INIT.name,
            TemplateInstanceStatus.INSTANCING.name
        )
        return Result(
            instanceFacadeService.listTemplateInstanceTask(
                projectId = projectId,
                templateId = templateId,
                statusList = statusList
            )
        )
    }

    override fun retryTemplateInstanceTask(
        userId: String,
        projectId: String,
        baseId: String
    ): Result<String> {
        return Result(
            instanceFacadeService.retryTemplateInstanceTask(
                userId = userId,
                projectId = projectId,
                baseId = baseId
            )
        )
    }

    override fun getTemplateInstanceTaskDetail(
        userId: String,
        projectId: String,
        baseId: String,
        status: String?
    ): Result<PipelineTemplateInstancesTaskDetail> {
        val statusList = status?.split(",") ?: listOf(
            TemplateInstanceStatus.INIT.name,
            TemplateInstanceStatus.INSTANCING.name
        )
        return Result(
            instanceFacadeService.getTemplateInstanceTaskDetail(
                projectId = projectId,
                baseId = baseId,
                statusList = statusList
            )
        )
    }
}
