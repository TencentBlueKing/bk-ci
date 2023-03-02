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
 */

package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.service.iam.PermissionResourceCallbackService
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectAuthCallBackResource

class RbacPermissionResourceCallbackService constructor(
    private val client: Client,
    private val authResourceService: AuthResourceService,
    private val objectMapper: ObjectMapper
) : PermissionResourceCallbackService {

    override fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO {
        val response = client.get(ServiceProjectAuthCallBackResource::class).projectInfo(
            token = token,
            callBackInfo = callBackInfo
        )
        return buildResult(callBackInfo.method, response.toString())
    }

    override fun getInstanceByResource(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val resourceType = callBackInfo.type
        return when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                val projectId = callBackInfo.filter.parent?.id ?: ""
                listInstance(
                    projectId = projectId,
                    resourceType = resourceType,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt()
                )
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                fetchInstance(
                    resourceType = resourceType,
                    iamResourceCodes = ids
                )
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                val projectId = callBackInfo.filter.parent?.id ?: ""
                val keyword = callBackInfo.filter.keyword
                searchInstance(
                    projectId = projectId,
                    resourceType = resourceType,
                    keyword = keyword,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt()
                )
            }
            else ->
                null
        }
    }

    private fun listInstance(
        projectId: String,
        resourceType: String,
        offset: Int,
        limit: Int
    ): ListInstanceResponseDTO {
        val instanceInfoList = authResourceService.list(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = null,
            limit = limit,
            offset = offset
        ).map {
            val entity = InstanceInfoDTO()
            entity.id = it.iamResourceCode
            entity.displayName = it.resourceName
            entity
        }
        val result = ListInstanceInfo()
        return if (instanceInfoList.isEmpty()) {
            result.buildListInstanceFailResult()
        } else {
            result.buildListInstanceResult(instanceInfoList, instanceInfoList.size.toLong())
        }
    }

    private fun fetchInstance(
        resourceType: String,
        iamResourceCodes: List<String>
    ): FetchInstanceInfoResponseDTO {
        val instanceInfoList = authResourceService.listByIamCodes(
            resourceType = resourceType,
            iamResourceCodes = iamResourceCodes
        ).map {
            val entity = InstanceInfoDTO()
            entity.id = it.iamResourceCode
            entity.displayName = it.resourceName
            entity
        }
        val result = FetchInstanceInfo()

        return if (instanceInfoList.isEmpty()) {
            result.buildFetchInstanceFailResult()
        } else {
            result.buildFetchInstanceResult(instanceInfoList)
        }
    }

    private fun searchInstance(
        projectId: String,
        resourceType: String,
        keyword: String,
        offset: Int,
        limit: Int
    ): SearchInstanceInfo {
        val instanceInfoList = authResourceService.list(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = keyword,
            limit = limit,
            offset = offset
        ).map {
            val entity = InstanceInfoDTO()
            entity.id = it.iamResourceCode
            entity.displayName = it.resourceName
            entity
        }
        val result = SearchInstanceInfo()
        return if (instanceInfoList.isEmpty()) {
            result.buildSearchInstanceFailResult()
        } else {
            result.buildSearchInstanceResult(instanceInfoList, instanceInfoList.size.toLong())
        }
    }

    private fun buildResult(method: CallbackMethodEnum, response: String): CallbackBaseResponseDTO {
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
}
