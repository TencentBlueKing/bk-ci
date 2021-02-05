package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.common.auth.callback.AuthConstants.KEYWORD_MIN_SIZE
import com.tencent.devops.common.auth.callback.AuthConstants.KEYWORD_SHORT
import com.tencent.devops.common.auth.callback.AuthConstants.KEYWORD_SHORT_MESSAGE
import com.tencent.devops.common.auth.callback.AuthConstants.MAX_LIMIT
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO as CallbackBaseResponseDTO1

@Service
class ResourceService @Autowired constructor(
    val objectMapper: ObjectMapper,
    val remoteAuthService: RemoteAuthService,
    val authPipelineService: AuthPipelineService,
    val authNodeService: AuthNodeService,
    val authRepositoryService: AuthRepositoryService,
    val authEnvService: AuthEnvService,
    val authCertService: AuthCertService,
    val authCredentialService: AuthCredentialService,
    val callbackService: CallBackService,
    val authHttpClientService: AuthHttpClientService
) {

    fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO1 {
        checkToken(token)
        val projectInfo = callbackService.getResource(AuthResourceType.PROJECT.value)
        val request =  authHttpClientService.buildPost(projectInfo!!.path, authHttpClientService.getJsonRequest(callBackInfo), projectInfo!!.gateway)
        val response = authHttpClientService.request(request, "调用回调接口失败")
        return objectMapper.readValue(response)
    }

    fun getInstanceByResource(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO1? {
        checkToken(token)
        checkoutParentType(callBackInfo.filter.parent.type)
        if (callBackInfo.method == CallbackMethodEnum.SEARCH_INSTANCE) {
            if (!checkKeyword(callBackInfo.filter.keyword)) {
                var result: SearchInstanceInfo = SearchInstanceInfo()

                if (!checkKeyword(callBackInfo.filter.keyword)) {
                    return result.buildSearchInstanceKeywordFailResult()
                }
            }
        }

        val actionType = callBackInfo.type
        val resourceType = if (actionType.contains("env_node")) {
            AuthResourceType.ENVIRONMENT_ENV_NODE.value
        } else {
            ActionUtils.actionType(actionType)
        }

        val resourceInfo = callbackService.getResource(resourceType)
        if (resourceInfo == null) {
            logger.warn("action $actionType not find resourceInfo, resourceType: $resourceType")
            return null
        }

        val request =  authHttpClientService.buildPost(resourceInfo!!.path, authHttpClientService.getJsonRequest(callBackInfo), resourceInfo!!.gateway)
        val response = authHttpClientService.request(request, "调用回调接口失败")
        return objectMapper.readValue(response)

    }

    fun getResource(
        input: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO1? {
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
        } else if (method == CallbackMethodEnum.SEARCH_INSTANCE) {
            return searchResouceInstances(input, resourceType)
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
            AuthResourceType.PIPELINE_DEFAULT.value -> result = authPipelineService.getPipelineInfo(ids)
            AuthResourceType.CODE_REPERTORY.value -> result = authRepositoryService.getRepositoryInfo(ids)
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = authEnvService.getEnvInfo(ids)
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = authNodeService.getNodeInfo(ids)
            AuthResourceType.TICKET_CREDENTIAL.value -> result = authCredentialService.getCredentialInfo(ids)
            AuthResourceType.TICKET_CERT.value -> result = authCertService.getCertInfo(ids)
            else -> null
        }
        return result
    }

    fun getResourceList(
        input: CallbackRequestDTO,
        resourceType: String
    ): ListInstanceResponseDTO? {
        checkoutParentType(input.filter.parent.type)
        val projectId = input.filter.parent.id
        val page = input.page

        var offset = 0
        var limit = MAX_LIMIT
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        var result: ListInstanceResponseDTO? = null
        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = authPipelineService.getPipeline(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            AuthResourceType.CODE_REPERTORY.value -> result = authRepositoryService.getRepository(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = authEnvService.getEnv(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = authNodeService.getNode(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            AuthResourceType.TICKET_CREDENTIAL.value -> result = authCredentialService.getCredential(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            AuthResourceType.TICKET_CERT.value -> result = authCertService.getCert(
                    projectId = projectId,
                    offset = offset,
                    limit = limit
            )
            else -> null
        }
        return result
    }

    fun searchResouceInstances(
        input: CallbackRequestDTO,
        resourceType: String
    ): SearchInstanceResponseDTO? {
        checkoutParentType(input.filter.parent.type)
        val projectId = input.filter.parent.id
        val page = input.page

        var offset = 0
        var limit = MAX_LIMIT
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        var result: SearchInstanceInfo = SearchInstanceInfo()

        if (!checkKeyword(input.filter.keyword)) {
            return result.buildSearchInstanceKeywordFailResult()
        }

        val keyword = input.filter.keyword

        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = authPipelineService.searchPipeline(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            AuthResourceType.CODE_REPERTORY.value -> result = authRepositoryService.searchRepositoryInstances(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = authEnvService.searchEnv(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = authNodeService.searchNode(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            AuthResourceType.TICKET_CERT.value -> result = authCertService.searchCert(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            AuthResourceType.TICKET_CREDENTIAL.value -> result = authCredentialService.searchCredential(
                    projectId = projectId,
                    keyword = keyword,
                    limit = limit,
                    offset = offset
            )
            else -> null
        }

        if (result?.data?.count!! > 100L) {
            return result.buildSearchInstanceResultFailResult()
        }

        return result
    }

    private fun checkToken(token: String) {
        if (!remoteAuthService.checkToken(token)) {
            logger.warn("auth callBack checkToken is fail $token")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.TOKEN_TICKET_FAIL))
        }
    }

    private fun checkoutParentType(type: String): Boolean {
        if (type != AuthResourceType.PROJECT.value) {
            throw ParamBlankException(AuthMessageCode.PARENT_TYPE_FAIL)
        }
        return true
    }

    private fun checkKeyword(keyword: String): Boolean {
        if (keyword.length < KEYWORD_MIN_SIZE) {
            return false
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
