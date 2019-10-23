package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwPipelineResource
import com.tencent.devops.process.api.ServicePipelineResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineResourceImpl @Autowired constructor(private val client: Client) : ApigwPipelineResource {

    override fun status(userId: String, projectId: String, pipelineId: String): Result<Pipeline?> {
        logger.info("Get a pipeline status at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).status(
            userId,
            projectId,
            pipelineId
        )
    }

    override fun create(userId: String, projectId: String, pipeline: Model): Result<PipelineId> {
        logger.info("Create a pipeline at project:$projectId with model: $pipeline")
        return client.get(ServicePipelineResource::class).create(
            userId,
            projectId,
            pipeline,
            ChannelCode.BS
        )
    }

    override fun edit(userId: String, projectId: String, pipelineId: String, pipeline: Model): Result<Boolean> {
        logger.info("Edit a pipeline at project:$projectId, pipelineId:$pipelineId with model: $pipeline")
        return client.get(ServicePipelineResource::class).edit(
            userId,
            projectId,
            pipelineId,
            pipeline,
            ChannelCode.BS
        )
    }

    override fun get(userId: String, projectId: String, pipelineId: String): Result<Model> {
        logger.info("Get a pipeline at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).get(
            userId,
            projectId,
            pipelineId,
            ChannelCode.BS
        )
    }

    override fun delete(userId: String, projectId: String, pipelineId: String): Result<Boolean> {
        logger.info("Delete a pipeline at project:$projectId, pipelineId:$pipelineId")
        return client.get(ServicePipelineResource::class).delete(
            userId,
            projectId,
            pipelineId,
            ChannelCode.BS
        )
    }

    override fun getListByUser(userId: String, projectId: String, page: Int?, pageSize: Int?): Result<Page<Pipeline>> {
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

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineResourceImpl::class.java)
    }
}