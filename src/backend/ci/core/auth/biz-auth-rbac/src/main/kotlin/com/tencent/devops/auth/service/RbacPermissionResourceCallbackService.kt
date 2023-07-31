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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.constants.DataTypeEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceListDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchResourceTypeSchemaDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceListDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SchemaData
import com.tencent.bk.sdk.iam.dto.callback.response.SchemaProperties
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.service.iam.PermissionResourceCallbackService
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.FetchInstanceListData
import com.tencent.devops.common.auth.callback.FetchInstanceListInfo
import com.tencent.devops.common.auth.callback.FetchResourceTypeSchemaInfo
import com.tencent.devops.common.auth.callback.FetchResourceTypeSchemaProperties
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import java.util.concurrent.TimeUnit

class RbacPermissionResourceCallbackService constructor(
    private val authResourceService: AuthResourceService,
    private val resourceService: ResourceService
) : PermissionResourceCallbackService {

    /*获取项目名称*/
    private val projectNameCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String/*projectCode*/, String/*projectName*/>()

    override fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO {
        return resourceService.getProject(callBackInfo, token)
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
            CallbackMethodEnum.FETCH_INSTANCE_LIST -> {
                fetchInstanceList(
                    resourceType = callBackInfo.type ?: "",
                    startTime = callBackInfo.filter.startTime ?: null,
                    endTime = callBackInfo.filter.endTime ?: null,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt()
                )
            }
            CallbackMethodEnum.FETCH_RESOURCE_TYPE_SCHEMA -> {
                fetchResourceTypeSchema(resourceType = callBackInfo.type ?: "")
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
        val count = authResourceService.count(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = null
        )
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
            result.buildListInstanceResult(instanceInfoList, count)
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
        val count = authResourceService.count(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = keyword
        )
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
            result.buildSearchInstanceResult(instanceInfoList, count)
        }
    }

    private fun fetchResourceTypeSchema(
        resourceType: String
    ): FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties> {
        val result = FetchResourceTypeSchemaInfo()
        if (resourceType != AuthResourceType.PIPELINE_DEFAULT.value)
            return result.buildFetchResourceTypeSchemaFailResult()
        val schemaData = SchemaData<FetchResourceTypeSchemaProperties>()
        val projectIdProperties = SchemaProperties().apply {
            type = DataTypeEnum.STRING
            description = PROJECT_ID_CHINESE_DESCRIPTION
        }
        val projectNameProperties = SchemaProperties().apply {
            type = DataTypeEnum.STRING
            description = PROJECT_NAME_CHINESE_DESCRIPTION
        }
        val pipelineIdProperties = SchemaProperties().apply {
            type = DataTypeEnum.STRING
            description = PIPELINE_ID_CHINESE_DESCRIPTION
        }
        val pipelineNameProperties = SchemaProperties().apply {
            type = DataTypeEnum.STRING
            description = PIPELINE_NAME_CHINESE_DESCRIPTION
        }
        schemaData.apply {
            type = OBJECT_TYPE
            schemaProperties = FetchResourceTypeSchemaProperties(
                projectId = projectIdProperties,
                projectName = projectNameProperties,
                pipelineId = pipelineIdProperties,
                pipelineName = pipelineNameProperties
            )
        }
        return result.buildFetchResourceTypeSchemaResult(schemaData)
    }

    private fun fetchInstanceList(
        resourceType: String,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): FetchInstanceListDTO<FetchInstanceListData> {
        if (limit > MAX_LIMIT) {
            return FetchInstanceListInfo().buildFetchInstanceListFailResult(
                "a maximum of 1000 data items can be obtained"
            )
        }
        if (resourceType != AuthResourceType.PIPELINE_DEFAULT.value) {
            return FetchInstanceListInfo().buildFetchInstanceListFailResult("empty data")
        }
        val fetchInstanceListInfo = authResourceService.list(
            resourceType = resourceType,
            startTime = startTime,
            endTime = endTime,
            offset = offset,
            limit = limit
        ).map { it.toInstanceListDTO() }
        val count = authResourceService.countResourceByUpdateTime(
            resourceType = resourceType,
            startTime = startTime,
            endTime = endTime
        )
        return FetchInstanceListInfo().buildFetchInstanceListResult(fetchInstanceListInfo, count)
    }

    private fun AuthResourceInfo.toInstanceListDTO(): InstanceListDTO<FetchInstanceListData> {
        val projectName = projectNameCache.getIfPresent(projectCode) ?: run {
            val resourceName = authResourceService.get(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            ).resourceName
            projectNameCache.put(projectCode, resourceName)
            resourceName
        }
        return InstanceListDTO<FetchInstanceListData>().apply {
            id = iamResourceCode
            displayName = resourceName
            creator = createUser
            updater = updateUser
            createdAt = createTime.timestampmilli()
            updatedAt = updateTime.timestampmilli()
            bkIamPath = listOf("/${AuthResourceType.PROJECT.value},$projectCode/")
            schemaProperties = FetchInstanceListData(
                projectId = projectCode,
                projectName = projectName,
                pipelineId = resourceCode,
                pipelineName = resourceName
            )
            operator = createUser
            delete = false
        }
    }

    companion object {
        private const val PROJECT_ID_CHINESE_DESCRIPTION = "项目ID"
        private const val PROJECT_NAME_CHINESE_DESCRIPTION = "项目名称"
        private const val PIPELINE_ID_CHINESE_DESCRIPTION = "流水线ID"
        private const val PIPELINE_NAME_CHINESE_DESCRIPTION = "流水线名称"
        private const val OBJECT_TYPE = "object"
        private const val MAX_LIMIT = 1000
    }
}
