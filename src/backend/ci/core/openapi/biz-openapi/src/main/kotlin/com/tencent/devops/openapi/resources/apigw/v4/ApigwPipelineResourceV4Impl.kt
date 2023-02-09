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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwPipelineResourceV4
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val apiGatewayUtil: ApiGatewayUtil
) :
    ApigwPipelineResourceV4 {
    override fun status(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Pipeline?> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|status|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).status(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipeline: Model
    ): Result<PipelineId> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|create|$projectId")
        return client.get(ServicePipelineResource::class).create(
            userId = userId,
            projectId = projectId,
            pipeline = pipeline,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun edit(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: Model
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|edit|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).edit(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipeline = pipeline,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun updatePipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        modelAndSetting: PipelineModelAndSetting
    ): Result<DeployPipelineResult> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|update pipeline|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).updatePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            modelAndSetting = modelAndSetting,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun uploadPipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        modelAndSetting: PipelineModelAndSetting
    ): Result<PipelineId> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|upload pipeline|$projectId")
        return client.get(ServicePipelineResource::class).uploadPipeline(
            userId = userId,
            projectId = projectId,
            modelAndSetting = modelAndSetting,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun get(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Model> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|get|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).getWithPermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = apiGatewayUtil.getChannelCode(),
            checkPermission = true
        )
    }

    override fun getSetting(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineSetting> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|get setting|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).getSettingWithPermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = apiGatewayUtil.getChannelCode(),
            checkPermission = true
        )
    }

    override fun getBatch(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineIds: List<String>
    ): Result<List<Pipeline>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|get batch|$projectId|$pipelineIds")
        return client.get(ServicePipelineResource::class).getBatch(
            userId = userId,
            projectId = projectId,
            pipelineIds = pipelineIds,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun delete(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|delete|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).delete(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun copy(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: PipelineCopy
    ): Result<PipelineId> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|copy|$projectId|$pipelineId|$pipeline")
        return client.get(ServicePipelineResource::class).copy(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipeline = pipeline
        )
    }

    override fun getListByUser(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|get list by user|$projectId|$page|$pageSize")
        return client.get(ServicePipelineResource::class).list(
            userId = userId,
            projectId = projectId,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            channelCode = apiGatewayUtil.getChannelCode(),
            checkPermission = true
        )
    }

    override fun rename(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        name: PipelineName
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|rename|$projectId|$pipelineId|$name")
        return client.get(ServicePipelineResource::class).rename(userId, projectId, pipelineId, name)
    }

    override fun restore(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|restore|$projectId|$pipelineId")
        return client.get(ServicePipelineResource::class).restore(userId, projectId, pipelineId)
    }

    override fun saveSetting(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        setting: PipelineSetting
    ): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|save setting|$projectId|$pipelineId|$setting")
        return client.get(ServicePipelineResource::class).saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = setting,
            channelCode = apiGatewayUtil.getChannelCode()
        )
    }

    override fun searchByName(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineName: String?
    ): Result<List<PipelineIdAndName>> {
        logger.info("OPENAPI_PIPELINE_V4|$userId|search by name|$projectId|$pipelineName")
        return client.get(ServicePipelineResource::class).searchByName(
            userId = userId,
            projectId = projectId,
            pipelineName = pipelineName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineResourceV4Impl::class.java)
    }
}
