package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制资源获取服务
 */
@Service
class PipelineCopyResourceGetService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineViewDao: PipelineViewDao
) {
    fun getCredentialBasicInfo(
        userId: String,
        projectId: String,
        credentialId: String,
        expectExists: Boolean
    ): PipelineCopyResourceBasicInfo? {
        return getResource(
            projectId = projectId,
            resourceName = credentialId,
            expectExists = expectExists
        ) {
            client.get(ServiceCredentialResource::class).getBasicInfo(
                userId = userId,
                projectId = projectId,
                credentialId = credentialId
            ).data?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = credentialId,
                    resourceName = credentialId
                )
            }
        }
    }

    fun getRepositoryByName(
        userId: String,
        projectId: String,
        repositoryName: String,
        expectExists: Boolean
    ): PipelineCopyResourceBasicInfo? {
        return getResource(
            projectId = projectId,
            resourceName = repositoryName,
            expectExists = expectExists
        ) {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repositoryName,
                repositoryType = RepositoryType.NAME
            ).data?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = it.repoHashId!!,
                    resourceName = it.aliasName
                )
            }
        }
    }

    fun getNodeByName(
        userId: String,
        projectId: String,
        nodeName: String,
        expectExists: Boolean?
    ): PipelineCopyResourceBasicInfo? {
        return getResource(
            projectId = projectId,
            resourceName = nodeName,
            expectExists = expectExists
        ) {
            client.get(ServiceNodeResource::class).getNodeStatus(
                userId = userId,
                projectId = projectId,
                nodeHashId = null,
                nodeName = nodeName,
                agentHashId = null
            ).data?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = it.nodeHashId,
                    resourceName = it.displayName ?: it.name
                )
            }
        }
    }

    fun getEnvByName(
        userId: String,
        projectId: String,
        envName: String,
        expectExists: Boolean?
    ): PipelineCopyResourceBasicInfo? {
        return getResource(
            projectId = projectId,
            resourceName = envName,
            expectExists = expectExists
        ) {
            client.get(ServiceEnvironmentResource::class).getByName(
                userId = userId,
                projectId = projectId,
                envName = envName,
                checkPermission = false
            ).data?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = it.envHashId,
                    resourceName = it.name
                )
            }
        }
    }

    fun getPipelineViewByName(
        projectId: String,
        viewName: String,
        expectExists: Boolean?
    ): PipelineCopyResourceBasicInfo? {
        return getResource(
            projectId = projectId,
            resourceName = viewName,
            expectExists = expectExists
        ) {
            pipelineViewDao.fetchAnyByName(
                dslContext = dslContext,
                projectId = projectId,
                name = viewName,
                isProject = true
            )?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = it.id.toString(),
                    resourceName = it.name
                )
            }
        }
    }

    fun getPipelineLabelByGroupAndName(
        projectId: String,
        groupName: String,
        labelName: String,
        expectExists: Boolean?
    ): PipelineCopyResourceBasicInfo? {
        val resourceName = "$groupName-$labelName"
        return getResource(
            projectId = projectId,
            resourceName = resourceName,
            expectExists = expectExists
        ) {
            val targetGroup = pipelineGroupDao.getByName(
                dslContext = dslContext,
                projectId = projectId,
                name = groupName
            ) ?: return@getResource null
            pipelineLabelDao.getByName(
                dslContext = dslContext,
                projectId = projectId,
                groupId = targetGroup.id,
                name = labelName
            )?.let {
                PipelineCopyResourceBasicInfo(
                    resourceId = HashUtil.encodeLongId(it.id),
                    resourceName = it.name
                )
            }
        }
    }

    /**
     * 验证目标项目资源名称是否符合预期。
     *
     * expectExists = true: 目标资源必须存在,用于复用或替换场景。
     * expectExists = false: 目标资源必须不存在,用于创建新资源前的冲突检查。
     * expectExists = null: 只查询资源,不做存在性校验。
     */
    private fun <T> getResource(
        projectId: String,
        resourceName: String,
        expectExists: Boolean?,
        get: () -> T?
    ): T? {
        val targetResource = try {
            get()
        } catch (ignored: RemoteServiceException) {
            null
        }
        if (expectExists == true && targetResource == null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_NOT_EXISTS,
                params = arrayOf(projectId, resourceName)
            )
        }
        if (expectExists == false && targetResource != null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EXISTS,
                params = arrayOf(projectId, resourceName)
            )
        }
        return targetResource
    }
}
