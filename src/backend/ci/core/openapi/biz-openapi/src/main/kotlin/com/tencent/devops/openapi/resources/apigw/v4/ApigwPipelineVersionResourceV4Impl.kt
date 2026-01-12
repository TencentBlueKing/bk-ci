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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwPipelineVersionResourceV4
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import com.tencent.devops.process.api.service.ServicePipelineVersionResource
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineVersionResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val apiGatewayUtil: ApiGatewayUtil
) : ApigwPipelineVersionResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineVersionResourceV4Impl::class.java)
    }

    override fun getPipelineVersionDetail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineDetail> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|getPipelineVersionDetail|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).getPipelineVersionDetail(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    override fun preFetchDraftVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PrefetchReleaseResult> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|preFetchDraftVersion|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).preFetchDraftVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    override fun releaseDraftVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        request: PipelineVersionReleaseRequest
    ): Result<DeployPipelineResult> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|releaseDraftVersion|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).releaseDraftVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            request = request
        )
    }

    override fun createPipelineFromTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: TemplateInstanceCreateRequest
    ): Result<DeployPipelineResult> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|createPipelineFromTemplate|$projectId|request=$request")
        return client.get(ServicePipelineVersionResource::class).createPipelineFromTemplate(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun getVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineVersionWithModel> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|getVersion|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).getVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    override fun previewCode(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<PreviewResponse> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|previewCode|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).previewCode(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    override fun savePipelineDraft(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        modelAndYaml: PipelineVersionWithModelRequest
    ): Result<DeployPipelineResult> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|savePipelineDraft|$projectId|$modelAndYaml")
        return client.get(ServicePipelineVersionResource::class).savePipelineDraft(
            userId = userId,
            projectId = projectId,
            modelAndYaml = modelAndYaml
        )
    }

    override fun versionCreatorList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<String>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|versionCreatorList|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).versionCreatorList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize
        )
    }

    override fun versionList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        fromVersion: Int?,
        versionName: String?,
        creator: String?,
        description: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineVersionWithInfo>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|versionList|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).versionList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            fromVersion = fromVersion,
            versionName = versionName,
            creator = creator,
            description = description,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getPipelineOperationLogs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        creator: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineOperationDetail>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|getPipelineOperationLogs|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).getPipelineOperationLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            page = page,
            pageSize = pageSize
        )
    }

    override fun operatorList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<String>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|operatorList|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).operatorList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    override fun rollbackDraftFromVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineVersionSimple> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|rollbackDraftFromVersion|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).rollbackDraftFromVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    override fun exportPipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?,
        storageType: String?
    ): Response {
        logger.info("OPENAPI_PIPELINE_V4|$userId|exportPipeline|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).exportPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            storageType = storageType
        )
    }

    override fun resetBuildNo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|resetBuildNo|$projectId|$pipelineId")
        return client.get(ServicePipelineVersionResource::class).resetBuildNo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    override fun exportPipelineAll(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        storageType: PipelineStorageType?,
        page: Int?
    ): Response {
        return client.get(ServicePipelineVersionResource::class).exportPipelineAll(
            userId = userId,
            projectId = projectId,
            storageType = storageType,
            page = page
        )
    }
}
