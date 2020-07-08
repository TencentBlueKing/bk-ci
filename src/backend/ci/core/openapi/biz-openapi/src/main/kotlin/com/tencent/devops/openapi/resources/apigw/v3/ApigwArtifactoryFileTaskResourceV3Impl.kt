package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryFileTaskResource
import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwArtifactoryFileTaskResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwArtifactoryFileTaskResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwArtifactoryFileTaskResourceV3 {
    override fun createFileTask(userId: String, projectId: String, pipelineId: String, buildId: String, createFileTaskReq: CreateFileTaskReq): Result<String> {
        return client.get(ServiceArtifactoryFileTaskResource::class).createFileTask(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            createFileTaskReq = createFileTaskReq
        )
    }

    override fun getStatus(userId: String, projectId: String, pipelineId: String, buildId: String, taskId: String): Result<FileTaskInfo?> {
        return client.get(ServiceArtifactoryFileTaskResource::class).getStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
    }

    override fun clearFileTask(appCode: String?, apigwType: String?, userId: String, projectId: String, pipelineId: String, buildId: String, taskId: String): Result<Boolean> {
        return client.get(ServiceArtifactoryFileTaskResource::class).clearFileTask(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwArtifactoryFileTaskResourceV3Impl::class.java)
    }

}