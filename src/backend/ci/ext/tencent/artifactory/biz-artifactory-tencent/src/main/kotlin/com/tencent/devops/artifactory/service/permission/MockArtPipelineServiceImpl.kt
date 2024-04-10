package com.tencent.devops.artifactory.service.permission

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelineResource

class MockArtPipelineServiceImpl(
    private val client: Client
) : PipelineService(client) {
    override fun validatePermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?,
        message: String?
    ) = Unit

    override fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?
    ) = true

    override fun filterPipeline(user: String, projectId: String): List<String> {
        val pipelineInfos = client.get(ServicePipelineResource::class).list(
            userId = user,
            projectId = projectId,
            page = 0,
            pageSize = 5000,
            channelCode = ChannelCode.BS,
            checkPermission = false
        ).data?.records

        return if (!pipelineInfos.isNullOrEmpty()) {
            pipelineInfos.map { it.pipelineId }
        } else {
            emptyList()
        }
    }
}
