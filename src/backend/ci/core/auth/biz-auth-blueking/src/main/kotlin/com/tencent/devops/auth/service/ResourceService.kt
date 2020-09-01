package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.environment.api.RemoteEnvResource
import com.tencent.devops.environment.api.RemoteNodeResource
import com.tencent.devops.process.api.service.ServiceAuthPipelineResource
import com.tencent.devops.project.api.service.ServiceAuthProjectResource
import com.tencent.devops.repository.api.ServiceAuthRepositoryResource
import com.tencent.devops.ticket.api.ServiceAuthCredentialResource

@Service
class ResourceService @Autowired constructor(
    val client: Client,
    val remoteAuthService: RemoteAuthService
) {

    fun getProjectList(page: PageInfoDTO, method: CallbackMethodEnum, token: String): ListInstanceResponseDTO {
        logger.info("getProjectList method $method, page $page token $token")
        checkToken(token)
        val projectRecords =
            client.get(ServiceAuthProjectResource::class).list(page.offset!!.toInt(), page.limit!!.toInt()).data
        logger.info("projectRecords $projectRecords")
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.englishName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = count
        data.result = projectInfo
        result.code = 0L
        result.message = ""
        result.data = data
        logger.info("result $result")
        return result
    }

    fun getResourceList(
        projectId: String,
        actionType: String,
        method: CallbackMethodEnum,
        page: PageInfoDTO,
        token: String
    ): ListInstanceResponseDTO? {
        logger.info("getResourceList project[$projectId] method[$method], page[$page],token[$token],actionType[$actionType]")
        checkToken(token)
        val resourceType = if (actionType.contains("env_node")) {
            AuthResourceType.ENVIRONMENT_ENV_NODE.value
        } else {
            ActionUtils.actionType(actionType)
        }
        if (AuthResourceType.get(resourceType) == null) {
            logger.warn("getResourceList actionType is not exits,actionType $actionType, resourceType $resourceType")
            throw RuntimeException("资源类型不存在")
        }
        var result: ListInstanceResponseDTO? = null
        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = getPipeline(projectId, page)
            AuthResourceType.CODE_REPERTORY.value -> result = getRepository(projectId, page)
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = getEnv(projectId, page)
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = getNode(projectId, page)
            AuthResourceType.TICKET_CREDENTIAL.value -> result = getCredential(projectId, page)
            else -> null
        }
        return result
    }

    private fun getPipeline(projectId: String, page: PageInfoDTO): ListInstanceResponseDTO? {
        val pipelineInfos =
            client.get(ServiceAuthPipelineResource::class)
                .pipelineList(projectId, page.offset.toInt(), page.limit.toInt()).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        data.count = pipelineInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getRepository(projectId: String, page: PageInfoDTO): ListInstanceResponseDTO? {
        val repositoryInfos =
            client.get(ServiceAuthRepositoryResource::class)
                .listByProjects(setOf(projectId), page.offset.toInt(), page.limit.toInt()).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (repositoryInfos?.records == null) {
            logger.info("$projectId 项目下无代码库")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos?.count}")
        data.count = repositoryInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getCredential(projectId: String, page: PageInfoDTO): ListInstanceResponseDTO? {
        val credentialInfos =
            client.get(ServiceAuthCredentialResource::class)
                .list(projectId, page.offset.toInt(), page.limit.toInt()).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (credentialInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialRemark
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos?.count}")
        data.count = credentialInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getNode(projectId: String, page: PageInfoDTO): ListInstanceResponseDTO? {
        val nodeInfos =
            client.get(RemoteNodeResource::class)
                .listNodeByPage(projectId, page.offset.toInt(), page.limit.toInt()).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (nodeInfos?.records == null) {
            logger.info("$projectId 项目下无节点")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos?.count}")
        data.count = nodeInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getEnv(projectId: String, page: PageInfoDTO): ListInstanceResponseDTO? {
        val envInfos =
            client.get(RemoteEnvResource::class)
                .listEnvByPage(projectId, page.offset.toInt(), page.limit.toInt()).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (envInfos?.records == null) {
            logger.info("$projectId 项目下无环境")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        data.count = envInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun checkToken(token: String) {
        if (!remoteAuthService.checkToken(token)) {
            logger.warn("auth callBack checkToken is fail $token")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.TOKEN_TICKET_FAIL))
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}