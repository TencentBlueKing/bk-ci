/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.callback.AuthConstants.KEYWORD_MIN_SIZE
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO as CallbackBaseResponseDTO1

@Service
class ResourceService @Autowired constructor(
    val objectMapper: ObjectMapper,
    val callbackService: CallBackService,
    val authHttpClientService: AuthHttpClientService
) {

    fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO1 {
        val projectInfo = callbackService.getResource(AuthResourceType.PROJECT.value)
        val request = authHttpClientService.buildPost(
            path = projectInfo!!.path,
            requestBody = authHttpClientService.getJsonRequest(callBackInfo),
            gateway = projectInfo.gateway,
            token = token
        )
        val response = authHttpClientService.request(
            request,
            I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_FAILED_CALL_CALLBACK_API)
        )
        return buildResult(callBackInfo.method, response)
    }

    fun getInstanceByResource(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO1? {
//        checkoutParentType(callBackInfo.filter.parent.type)
        if (callBackInfo.method == CallbackMethodEnum.SEARCH_INSTANCE) {
            if (!checkKeyword(callBackInfo.filter.keyword)) {
                val result = SearchInstanceInfo()
                return result.buildSearchInstanceKeywordFailResult()
            }
        }

        val actionType = callBackInfo.type
        val resourceType = actionType

        val resourceInfo = callbackService.getResource(resourceType)
        if (resourceInfo == null) {
            logger.warn("action $actionType not find resourceInfo, resourceType: $resourceType")
            return null
        }

        val request = authHttpClientService.buildPost(
            path = resourceInfo.path,
            requestBody = authHttpClientService.getJsonRequest(callBackInfo),
            gateway = resourceInfo.gateway,
            token = token,
            system = resourceInfo.system
        )
        val response = authHttpClientService.request(
            request,
            I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_FAILED_CALL_CALLBACK_API)
        )

        logger.info("getInstanceByResource response: $response")

        return buildResult(callBackInfo.method, response)
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

    private fun buildResult(method: CallbackMethodEnum, response: String): CallbackBaseResponseDTO1 {
        return when (method) {
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                val searchResult = objectMapper.readValue<SearchInstanceInfo>(response)
                if (searchResult.data?.count!! > 100L) {
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
        private val logger = LoggerFactory.getLogger(ResourceService::class.java)
        const val DEFAULTSYSTEM = "ci"
    }
}
