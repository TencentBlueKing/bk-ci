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

package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwPipelineGroupResourceV3
import com.tencent.devops.process.api.service.ServicePipelineGroupResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineGroupResourceV3Impl @Autowired constructor(private val client: Client) :
    ApigwPipelineGroupResourceV3 {

    override fun getGroups(userId: String, projectId: String): Result<List<PipelineGroup>> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|get groups|$projectId")
        return client.get(ServicePipelineGroupResource::class).getGroups(
            userId = userId,
            projectId = projectId
        )
    }

    override fun addGroup(userId: String, pipelineGroup: PipelineGroupCreate): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|add group|$pipelineGroup")
        return client.get(ServicePipelineGroupResource::class).addGroup(
            userId = userId,
            pipelineGroup = pipelineGroup
        )
    }

    override fun updateGroup(userId: String, pipelineGroup: PipelineGroupUpdate): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|update group|$pipelineGroup")
        return client.get(ServicePipelineGroupResource::class).updateGroup(
            userId = userId,
            pipelineGroup = pipelineGroup
        )
    }

    override fun deleteGroup(userId: String, projectId: String, groupId: String): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|delete group|$projectId|$groupId")
        return client.get(ServicePipelineGroupResource::class).deleteGroup(
            userId = userId,
            projectId = projectId,
            groupId = groupId
        )
    }

    override fun addLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelCreate): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|add label|$projectId|$pipelineLabel")
        return client.get(ServicePipelineGroupResource::class).addLabel(
            userId = userId,
            projectId = projectId,
            pipelineLabel = pipelineLabel
        )
    }

    override fun deleteLabel(userId: String, projectId: String, labelId: String): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|delete label|$projectId|$labelId")
        return client.get(ServicePipelineGroupResource::class).deleteLabel(
            userId = userId,
            projectId = projectId,
            labelId = labelId
        )
    }

    override fun updateLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelUpdate): Result<Boolean> {
        logger.info("OPENAPI_PIPELINE_GROUP_V3|$userId|update label|$projectId|$pipelineLabel")
        return client.get(ServicePipelineGroupResource::class).updateLabel(
            userId = userId,
            projectId = projectId,
            pipelineLabel = pipelineLabel
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineGroupResourceV3Impl::class.java)
    }
}
