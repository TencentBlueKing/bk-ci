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

package com.tencent.devops.auth.service.migrate

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.request.FilterDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.RbacPermissionResourceService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 将资源迁移到权限中心
 */
@Suppress("LongParameterList")
class MigrateResourceService @Autowired constructor(
    private val resourceService: ResourceService,
    private val rbacCacheService: RbacCacheService,
    private val rbacPermissionResourceService: RbacPermissionResourceService,
    private val authResourceService: AuthResourceService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val tokenApi: AuthTokenApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode
) {
    fun migrateResource(projectCode: String) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource:$projectCode")

        val resourceTypes = rbacCacheService.listResourceTypes()
            .map { it.resourceType }
            .filterNot { noNeedToMigrateResourceType.contains(it) }

        logger.info("MigrateResourceService|resourceTypes:$resourceTypes")
        // 迁移各个资源类型下的资源
        resourceTypes.forEach { resourceType ->
            resourceCreateRelation(
                resourceType = resourceType, projectCode = projectCode
            )
        }
        logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource $projectCode")
    }

    @SuppressWarnings("MagicNumber")
    private fun resourceCreateRelation(resourceType: String, projectCode: String) {
        var offset = 0L
        val limit = 100L
        do {
            val resourceData = listInstance(
                offset = offset,
                limit = limit,
                resourceType = resourceType,
                projectCode = projectCode
            ) ?: return
            logger.info("MigrateResourceService|resourceData:$resourceData")
            val ids = resourceData.data.result.map { it.id }
            val instanceInfoList = fetchInstanceInfo(
                resourceType = resourceType,
                projectCode = projectCode,
                ids = ids
            ) ?: return
            instanceInfoList.data.map { it as InstanceInfoDTO }.forEach {
                val resourceCode =
                    migrateResourceCodeConverter.v3ToRbacResourceCode(
                        resourceType = resourceType,
                        resourceCode = it.id
                    )
                logger.info("MigrateResourceService|resourceCode:$resourceCode")
                authResourceService.getOrNull(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ) ?: run {
                    rbacPermissionResourceService.resourceCreateRelation(
                        userId = it.iamApprover[0],
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        resourceName = it.displayName
                    )
                }
            }
            offset += limit
        } while (resourceData.data.count == limit)
    }

    private fun listInstance(
        offset: Long,
        limit: Long,
        resourceType: String,
        projectCode: String
    ): ListInstanceResponseDTO? {
        val pathInfoDTO = PathInfoDTO().apply {
            type = AuthResourceType.PROJECT.value
            id = projectCode
        }
        val filterDTO = FilterDTO().apply {
            parent = pathInfoDTO
        }
        return resourceService.getInstanceByResource(
            callBackInfo = CallbackRequestDTO().apply {
                type = resourceType
                method = CallbackMethodEnum.LIST_INSTANCE
                filter = filterDTO
                page = PageInfoDTO().apply {
                    this.offset = offset
                    this.limit = limit
                }
            },
            token = tokenApi.getAccessToken(projectAuthServiceCode)
        ) as ListInstanceResponseDTO?
    }

    private fun fetchInstanceInfo(
        resourceType: String,
        projectCode: String,
        ids: List<String>
    ): FetchInstanceInfo? {
        val pathInfoDTO = PathInfoDTO().apply {
            type = AuthResourceType.PROJECT.value
            id = projectCode
        }
        val filterDTO = FilterDTO().apply {
            parent = pathInfoDTO
            idList = ids
        }
        return resourceService.getInstanceByResource(
            callBackInfo = CallbackRequestDTO().apply {
                type = resourceType
                method = CallbackMethodEnum.FETCH_INSTANCE_INFO
                filter = filterDTO
            },
            token = tokenApi.getAccessToken(projectAuthServiceCode)
        ) as FetchInstanceInfo?
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResourceService::class.java)
        private val noNeedToMigrateResourceType = listOf(
            AuthResourceType.CODECC_TASK.value,
            AuthResourceType.CODECC_IGNORE_TYPE.value,
            AuthResourceType.CODECC_RULE_SET.value,
            AuthResourceType.PIPELINE_GROUP.value,
            AuthResourceType.TURBO.value,
            AuthResourceType.PROJECT.value
        )
    }
}
