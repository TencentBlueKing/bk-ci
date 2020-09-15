package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.api.RemoteEnvResource
import com.tencent.devops.environment.api.RemoteNodeResource
import com.tencent.devops.process.api.service.ServiceAuthPipelineResource
import com.tencent.devops.project.api.service.ServiceAuthProjectResource
import com.tencent.devops.repository.api.ServiceAuthRepositoryResource
import com.tencent.devops.ticket.api.ServiceAuthCallbackResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ResourceService @Autowired constructor(
    val client: Client,
    val remoteAuthService: RemoteAuthService
) {

    fun getProjectInfo(callBackInfo: CallbackRequestDTO, method: CallbackMethodEnum, token: String): CallbackBaseResponseDTO {
        checkToken(token)
        if (method == CallbackMethodEnum.LIST_INSTANCE) {
            return getProjectList(callBackInfo.page, method, token)
        } else if (method == CallbackMethodEnum.FETCH_INSTANCE_INFO) {
            val ids = callBackInfo.filter.idList.map { it.toString() }
            return getProjectInfo(ids, callBackInfo.filter.attributeList)
        }
        return getProjectList(callBackInfo.page, method, token)
    }

    fun getResource(
        input: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        logger.info("getResourceList input[$input] ,token[$token]")
        val actionType = input.type
        val method = input.method
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

        if (method == CallbackMethodEnum.LIST_INSTANCE) {
            return getResourceList(input, resourceType)
        } else if (method == CallbackMethodEnum.FETCH_INSTANCE_INFO) {
            return getResourceInfos(input, resourceType)
        }
        return getResourceList(input, resourceType)
    }

    fun getResourceInfos(
        input: CallbackRequestDTO,
        resourceType: String
    ): FetchInstanceInfoResponseDTO? {
        val ids = input.filter.idList
        val actionType = input.type
        if (ids == null || ids.isEmpty()) {
            logger.warn("getResourceInfos ids is empty|$input| $actionType")
            throw RuntimeException("资源类型不存在")
        }
        var result: FetchInstanceInfoResponseDTO? = null
        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = getPipelineInfo(ids)
            AuthResourceType.CODE_REPERTORY.value -> result = getRepositoryInfo(ids)
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = getEnvInfo(ids)
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = getNodeInfo(ids)
            AuthResourceType.TICKET_CREDENTIAL.value -> result = getCredentialInfo(ids)
            AuthResourceType.TICKET_CERT.value -> result = getCertInfo(ids)
            else -> null
        }
        return result
    }

    fun getResourceList(
        input: CallbackRequestDTO,
        resourceType: String
    ): ListInstanceResponseDTO? {
        val projectId = input.filter.parent.id
        val page = input.page

        var offset = 0
        var limit = 10
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        var result: ListInstanceResponseDTO? = null
        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = getPipeline(projectId, offset, limit)
            AuthResourceType.CODE_REPERTORY.value -> result = getRepository(projectId, offset, limit)
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = getEnv(projectId, offset, limit)
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = getNode(projectId, offset, limit)
            AuthResourceType.TICKET_CREDENTIAL.value -> result = getCredential(projectId, offset, limit)
            AuthResourceType.TICKET_CERT.value -> result = getCert(projectId, offset, limit)
            else -> null
        }
        return result
    }

    private fun getPipeline(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val pipelineInfos =
            client.get(ServiceAuthPipelineResource::class)
                .pipelineList(projectId, offset, limit).data
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

    private fun getPipelineInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val pipelineInfos =
                client.get(ServiceAuthPipelineResource::class)
                        .pipelineInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfoResponseDTO()

        if (pipelineInfos == null || pipelineInfos.isEmpty()) {
            logger.info("$ids 未匹配到启用流水线")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun getRepository(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val repositoryInfos =
            client.get(ServiceAuthRepositoryResource::class)
                .listByProjects(projectId, offset, limit).data
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

    private fun getRepositoryInfo(hashId: List<Any>?): FetchInstanceInfoResponseDTO? {
        val repositoryInfos =
                client.get(ServiceAuthRepositoryResource::class)
                        .getInfos(hashId as List<String>).data
        val result = FetchInstanceInfoResponseDTO()
        if (repositoryInfos == null || repositoryInfos.isEmpty()) {
            logger.info("$hashId 未匹配到代码库")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun getCredential(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val credentialInfos =
            client.get(ServiceAuthCallbackResource::class)
                .listCredential(projectId, offset, limit).data
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
            entity.displayName = it.credentialId
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

    private fun getCredentialInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val credentialInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .getCredentialInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfoResponseDTO()
        if (credentialInfos == null || credentialInfos.isEmpty()) {
            logger.info("$ids 无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun getCert(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .listCert(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (certInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        data.count = certInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getCertInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .getCertInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfoResponseDTO()
        if (certInfos == null || certInfos.isEmpty()) {
            logger.info("$ids 无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun getNode(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val nodeInfos =
            client.get(RemoteNodeResource::class)
                .listNodeByPage(projectId, offset, limit).data
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

    private fun getEnv(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val envInfos =
            client.get(RemoteEnvResource::class)
                .listEnvByPage(projectId, offset, limit).data
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

    private fun getNodeInfo(hashIds: List<Any>?): FetchInstanceInfoResponseDTO? {
        val nodeInfos =
                client.get(RemoteNodeResource::class)
                        .getNodeInfos(hashIds as List<String>).data
        val result = FetchInstanceInfoResponseDTO()
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            logger.info("$hashIds 无节点")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun getEnvInfo(hashId: List<Any>?): FetchInstanceInfoResponseDTO? {
        val envInfos =
                client.get(RemoteEnvResource::class)
                        .getEnvInfos(hashId as List<String>).data
        val result = FetchInstanceInfoResponseDTO()
        if (envInfos == null || envInfos.isEmpty()) {
            logger.info("$hashId 下无环境")
            result.code = 0
            result.message = "无数据"
            result.data = emptyList()
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos.size.toLong()}")
        result.code = 0L
        result.message = ""
        result.data = entityInfo.toList()
        return result
    }

    private fun checkToken(token: String) {
        if (!remoteAuthService.checkToken(token)) {
            logger.warn("auth callBack checkToken is fail $token")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.TOKEN_TICKET_FAIL))
        }
    }

    private fun getProjectList(page: PageInfoDTO?, method: CallbackMethodEnum, token: String): ListInstanceResponseDTO {
        logger.info("getProjectList method $method, page $page token $token")
        var offset = 0
        var limit = 10
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val projectRecords =
                client.get(ServiceAuthProjectResource::class).list(offset, limit).data
        logger.info("projectRecords $projectRecords")
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
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

    private fun getProjectInfo(idList: List<String>, attrs: List<String>): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList] attrs[$attrs]")
        val ids = idList.toSet()
        val projectInfo = client.get(ServiceAuthProjectResource::class).getByIds(ids).data
        logger.info("projectRecords $projectInfo")
        val entityList = mutableListOf<InstanceInfoDTO>()
        projectInfo?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            entityList.add(entity)
        }
        logger.info("entityInfo $entityList")
        val result = FetchInstanceInfoResponseDTO()
        result.code = 0
        result.message = ""
        result.data = entityList.toList()
        return result
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}