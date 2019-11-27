package com.tencent.devops.process.service.ipt

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IptRepoService @Autowired constructor(
    private val client: Client,
    private val authPermissionApi: AuthPermissionApi,
    private val dslContext: DSLContext,
    private val pipelineBuildVarDao: PipelineBuildVarDao
) {

    fun getCommitBuildArtifactorytInfo(
        projectId: String,
        pipelineId: String,
        userId: String,
        commitId: String
    ): IptBuildArtifactoryInfo {
        logger.info("get commit build artifactory info: $projectId, $pipelineId, $userId, $commitId")
        checkPermission(projectId, pipelineId, userId)

        val buildId = getBuildByCommitId(projectId, pipelineId) ?:
            throw RuntimeException("can not find build for commit")

        val searchProperty = listOf(Property("buildId", buildId), Property("pipelineId", pipelineId))
        val fileList = client.get(ServiceArtifactoryResource::class)
            .search(projectId, null, null, searchProperty).data?.records ?: listOf()
        return IptBuildArtifactoryInfo(buildId, fileList)
    }

    private fun getBuildByCommitId(projectId: String, pipelineId: String): String? {
        val headCommit = pipelineBuildVarDao.getVarsByProjectAndPipeline(dslContext, projectId, pipelineId)?.filter {
            it.key == "DEVOPS_GIT_REPO_HEAD_COMMIT_ID"
        }
        return headCommit?.firstOrNull()?.buildId
    }

    private fun checkPermission(projectId: String, pipelineId: String, userId: String) {
        val result = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = BSPipelineAuthServiceCode(),
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            permission = AuthPermission.DOWNLOAD
        )
        if (!result) throw RuntimeException("用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

}