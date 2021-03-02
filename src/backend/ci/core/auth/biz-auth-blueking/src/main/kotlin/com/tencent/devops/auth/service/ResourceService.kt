package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.callback.AuthConstants.KEYWORD_MIN_SIZE
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO as CallbackBaseResponseDTO1

@Service
class ResourceService @Autowired constructor(
    val objectMapper: ObjectMapper,
    val remoteAuthService: RemoteAuthService,
    val callbackService: CallBackService,
    val authHttpClientService: AuthHttpClientService
) {

    fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO1 {
        checkToken(token)
        val projectInfo = callbackService.getResource(AuthResourceType.PROJECT.value)
        val request = authHttpClientService.buildPost(projectInfo!!.path, authHttpClientService.getJsonRequest(callBackInfo), projectInfo!!.gateway)
        val response = authHttpClientService.request(request, "调用回调接口失败")
        return buildResult(callBackInfo.method, response)
    }

    fun getInstanceByResource(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO1? {
        checkToken(token)
        checkoutParentType(callBackInfo.filter.parent.type)
        if (callBackInfo.method == CallbackMethodEnum.SEARCH_INSTANCE) {
            if (!checkKeyword(callBackInfo.filter.keyword)) {
                var result = SearchInstanceInfo()
                return result.buildSearchInstanceKeywordFailResult()
            }
        }

        val actionType = callBackInfo.type
        val resourceType = findEnvNode(actionType)

        val resourceInfo = callbackService.getResource(resourceType)
        if (resourceInfo == null) {
            logger.warn("action $actionType not find resourceInfo, resourceType: $resourceType")
            return null
        }

        val request = authHttpClientService.buildPost(resourceInfo!!.path, authHttpClientService.getJsonRequest(callBackInfo), resourceInfo!!.gateway)
        val response = authHttpClientService.request(request, "调用回调接口失败")

        logger.info("getInstanceByResource response: $response")

        return buildResult(callBackInfo.method, response)
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

    private fun findEnvNode(actionType: String): String {
        return if (actionType.contains("env_node")) {
            AuthResourceType.ENVIRONMENT_ENV_NODE.value
        } else {
            ActionUtils.actionType(actionType)
        }
    }

    private fun buildResult(method: CallbackMethodEnum, response: String): CallbackBaseResponseDTO1 {
        return when (method) {
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                val searchResult = objectMapper.readValue<SearchInstanceInfo>(response)
                if (searchResult?.data?.count!! > 100L) {
                    searchResult.buildSearchInstanceResultFailResult()
                } else {
                    searchResult
                }
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> objectMapper.readValue<FetchInstanceInfoResponseDTO>(response)
            CallbackMethodEnum.LIST_INSTANCE -> objectMapper.readValue<ListInstanceResponseDTO>(response)
            else -> objectMapper.readValue(response)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
