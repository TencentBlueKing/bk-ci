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
package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwPipelineResourceV3
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.PipelineWithModel
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineName
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineResourceV3Impl @Autowired constructor(private val client: Client) :
    ApigwPipelineResourceV3 {

    override fun status(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Pipeline?> {
        logger.info("Get a pipeline status at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).status(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipeline: Model
    ): Result<PipelineId> {
        logger.info("Create a pipeline at project:$projectId with model: $pipeline")
        return client.get(ServicePipelineResource::class).create(
            userId = userId,
            projectId = projectId,
            pipeline = pipeline,
            channelCode = ChannelCode.BS
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
        logger.info("Edit a pipeline at project:$projectId, pipelineId:$pipelineId with model: $pipeline")
        return client.get(ServicePipelineResource::class).edit(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipeline = pipeline,
            channelCode = ChannelCode.BS
        )
    }

    override fun get(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Model> {
        logger.info("Get a pipeline at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).get(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getBatch(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineIds: List<String>
    ): Result<List<PipelineWithModel>> {
        logger.info("Get batch pipelines at project:$projectId, pipelineIds:$pipelineIds")
        return client.get(ServicePipelineResource::class).getBatch(
            userId = userId,
            projectId = projectId,
            pipelineIds = pipelineIds,
            channelCode = ChannelCode.BS
        )
    }

    override fun delete(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Boolean> {
        logger.info("Delete a pipeline at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).delete(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
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
        logger.info("get pipelines by user, userId:$userId")
        return client.get(ServicePipelineResource::class).list(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS,
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
        logger.info("rename: userId[$userId] projectId[$projectId] pipelineId[$pipelineId] name[$name]")
        return client.get(ServicePipelineResource::class).rename(userId, projectId, pipelineId, name)
    }

    override fun restore(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<Boolean> {
        logger.info("restore: userId[$userId] projectId[$projectId] pipelineId[$pipelineId]")
        return client.get(ServicePipelineResource::class).restore(userId, projectId, pipelineId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineResourceV3Impl::class.java)
    }
}