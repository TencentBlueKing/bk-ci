package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.RepoAuthCopyResourceProp
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
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
    private val pipelineViewDao: PipelineViewDao
) {
    fun getCredentialBasicInfo(
        userId: String,
        projectId: String,
        credentialId: String,
        expectExists: Boolean
    ): PipelineCopyTargetResource? {
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
                PipelineCopyTargetResource(
                    resourceId = credentialId,
                    resourceName = credentialId
                )
            }
        }
    }

    fun validateRepositoryProperties(resource: PipelineCopyTaskResource) {
        val targetResourceProperties = resource.targetResourceProp as? RepoAuthCopyResourceProp
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_REPOSITORY_PROPERTIES_EMPTY,
                params = arrayOf(resource.resourceType.name, resource.resourceName)
            )
        val resourceProperties = resource.resourceProperties as? RepoAuthCopyResourceProp
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_REPOSITORY_PROPERTIES_EMPTY,
                params = arrayOf(resource.resourceType.name, resource.resourceName)
            )
        if (resourceProperties.authType != targetResourceProperties.authType) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_REPOSITORY_AUTH_TYPE_NOT_MATCH,
                params = arrayOf(
                    resource.resourceType.name,
                    resource.resourceName,
                    resourceProperties.authType ?: "",
                    targetResourceProperties.authType ?: ""
                )
            )
        }
        if (targetResourceProperties.authType != RepoAuthType.OAUTH.name &&
            targetResourceProperties.authInfo.isNullOrBlank()
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_REPOSITORY_AUTH_INFO_EMPTY,
                params = arrayOf(
                    resource.resourceType.name,
                    resource.resourceName,
                    targetResourceProperties.authType ?: ""
                )
            )
        }
    }

    fun getRepositoryByName(
        userId: String,
        projectId: String,
        repositoryName: String,
        expectExists: Boolean
    ): PipelineCopyTargetResource? {
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
                PipelineCopyTargetResource(
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
    ): PipelineCopyTargetResource? {
        return getNode(
            userId = userId,
            projectId = projectId,
            nodeHashId = null,
            nodeName = nodeName,
            expectExists = expectExists
        )
    }

    fun getNodeByHashId(
        userId: String,
        projectId: String,
        nodeHashId: String,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
        return getNode(
            userId = userId,
            projectId = projectId,
            nodeHashId = nodeHashId,
            nodeName = null,
            expectExists = expectExists
        )
    }

    fun getEnvByName(
        userId: String,
        projectId: String,
        envName: String,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
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
                PipelineCopyTargetResource(
                    resourceId = it.envHashId,
                    resourceName = it.name
                )
            }
        }
    }

    fun getEnvByHashId(
        userId: String,
        projectId: String,
        envHashId: String,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
        return getResource(
            projectId = projectId,
            resourceName = envHashId,
            expectExists = expectExists
        ) {
            client.get(ServiceEnvironmentResource::class).get(
                userId = userId,
                projectId = projectId,
                envHashId = envHashId,
                checkPermission = false
            ).data?.let {
                PipelineCopyTargetResource(
                    resourceId = it.envHashId,
                    resourceName = it.name
                )
            }
        }
    }

    fun getPipelineGroupByName(
        projectId: String,
        viewName: String,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
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
                PipelineCopyTargetResource(
                    resourceId = it.id.toString(),
                    resourceName = it.name
                )
            }
        }
    }

    fun getPipelineLabelByName(
        projectId: String,
        labelName: String,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
        return getResource(
            projectId = projectId,
            resourceName = labelName,
            expectExists = expectExists
        ) {
            pipelineLabelDao.getByName(
                dslContext = dslContext,
                projectId = projectId,
                name = labelName
            )?.let {
                PipelineCopyTargetResource(
                    resourceId = HashUtil.encodeLongId(it.id),
                    resourceName = it.name
                )
            }
        }
    }

    private fun getNode(
        userId: String,
        projectId: String,
        nodeHashId: String?,
        nodeName: String?,
        expectExists: Boolean?
    ): PipelineCopyTargetResource? {
        val resourceName = nodeHashId ?: nodeName.orEmpty()
        return getResource(
            projectId = projectId,
            resourceName = resourceName,
            expectExists = expectExists
        ) {
            client.get(ServiceNodeResource::class).getNodeStatus(
                userId = userId,
                projectId = projectId,
                nodeHashId = nodeHashId,
                nodeName = nodeName,
                agentHashId = null
            ).data?.let {
                PipelineCopyTargetResource(
                    resourceId = it.nodeHashId,
                    resourceName = it.displayName ?: it.name
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
