package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitProjectPipelineDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class BuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitProjectPipelineDao: GitProjectPipelineDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BuildService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun retry(userId: String, gitProjectId: Long, buildId: String, taskId: String?): BuildId {
        logger.info("retry pipeline, gitProjectId: $gitProjectId, buildId: $buildId")
        val gitProjectPipeline = getProjectPipeline(gitProjectId)
        val gitEventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: throw CustomException(Response.Status.NOT_FOUND, "构建任务不存在，无法重试")
        val newBuildId = client.get(ServiceBuildResource::class).retry(userId, gitProjectPipeline.projectCode, gitProjectPipeline.pipelineId, buildId, taskId, channelCode).data!!

        gitRequestEventBuildDao.save(
                dslContext,
                gitEventBuild.eventId,
                gitEventBuild.normalizedYaml,
                gitEventBuild.originYaml,
                gitEventBuild.pipelineId,
                newBuildId.id,
                gitEventBuild.gitProjectId,
                gitEventBuild.branch,
                gitEventBuild.objectKind,
                gitEventBuild.description)

        return newBuildId
    }

    fun manualShutdown(userId: String, gitProjectId: Long, buildId: String): Boolean {
        logger.info("manualShutdown, gitProjectId: $gitProjectId, buildId: $buildId")
        val gitProjectPipeline = getProjectPipeline(gitProjectId)

        return client.get(ServiceBuildResource::class).manualShutdown(
                userId,
                gitProjectPipeline.projectCode,
                gitProjectPipeline.pipelineId,
                buildId,
                channelCode
        ).data!!
    }

    private fun getProjectPipeline(gitProjectId: Long) = gitProjectPipelineDao.get(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")
}
