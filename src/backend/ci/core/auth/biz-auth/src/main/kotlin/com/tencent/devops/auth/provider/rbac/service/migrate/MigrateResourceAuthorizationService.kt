/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.request.FilterDTO
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.callback.ListResourcesAuthorizationDTO
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 迁移资源授权
 */
@Service
class MigrateResourceAuthorizationService @Autowired constructor(
    private val resourceService: ResourceService,
    private val tokenApi: AuthTokenApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val permissionAuthorizationService: PermissionAuthorizationService,
    private val client: Client
) {
    fun migrateResourceAuthorization(projectCodes: List<String>): Boolean {
        logger.info("start to migrate resource authorization by project list:$projectCodes")
        executorService.submit {
            projectCodes.forEach {
                migrateResourceAuthorization(
                    projectCode = it
                )
            }
        }
        return true
    }

    fun migrateAllResourceAuthorization(): Boolean {
        logger.info("start to migrate all project resource authorization")
        executorService.submit {
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE / 2
            do {
                val migrateProjects = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = ProjectConditionDTO(),
                    limit = limit,
                    offset = offset
                ).data ?: break
                migrateProjects.forEach {
                    migrateResourceAuthorization(it.englishName)
                }
                offset += limit
            } while (migrateProjects.size == limit)
        }
        return true
    }

    private fun migrateResourceAuthorization(projectCode: String) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource authorization:$projectCode")
        try {
            val resourceTypes = listOf(
                AuthResourceType.PIPELINE_DEFAULT.value,
                AuthResourceType.ENVIRONMENT_ENV_NODE.value,
                AuthResourceType.CODE_REPERTORY.value
            )

            logger.info("MigrateResourceAuthorization|resourceTypes:$resourceTypes")
            // 迁移各个资源类型下的资源授权
            val traceId = MDC.get(TraceTag.BIZID)
            resourceTypes.forEach { resourceType ->
                CompletableFuture.supplyAsync(
                    {
                        MDC.put(TraceTag.BIZID, traceId)
                        migrateResourceAuthorization(
                            projectCode = projectCode,
                            resourceType = resourceType
                        )
                    },
                    executorService
                )
            }
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource $projectCode")
        }
    }

    private fun migrateResourceAuthorization(
        projectCode: String,
        resourceType: String
    ) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource authorization|$projectCode|$resourceType")
        try {
            createResourceAuthorization(
                resourceType = resourceType,
                projectCode = projectCode
            )
        } catch (ignore: Exception) {
            logger.error("Failed to migrate resource authorization|$projectCode|$resourceType", ignore)
            throw ignore
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate " +
                    "resource authorization|$projectCode|$resourceType"
            )
        }
    }

    private fun createResourceAuthorization(
        resourceType: String,
        projectCode: String
    ) {
        var offset = 0L
        val limit = 100L
        val resourceAuthorizationIds = mutableListOf<String>()
        do {
            val resourceAuthorizationData = listResourceAuthorization(
                offset = offset,
                limit = limit,
                resourceType = resourceType,
                projectCode = projectCode
            )?.data

            logger.info(
                "MigrateResourceAuthorizationService|projectCode:$projectCode|resourceType:$resourceType" +
                    "|resourceAuthorizationData:$resourceAuthorizationData"
            )
            if (resourceAuthorizationData == null || resourceAuthorizationData.result.isNullOrEmpty()) {
                return
            }
            permissionAuthorizationService.migrateResourceAuthorization(
                resourceAuthorizationList = resourceAuthorizationData.result.map {
                    ResourceAuthorizationDTO(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceName = it.resourceName,
                        resourceCode = it.resourceCode,
                        handoverTime = it.handoverTime,
                        handoverFrom = it.handoverFrom
                    )
                }
            )
            resourceAuthorizationIds.addAll(resourceAuthorizationData.result.map { it.resourceCode })
            offset += limit
        } while (resourceAuthorizationData!!.result.size.toLong() == limit)
        // 由于生产和灰度不是同时发布，可能会出现生产删除资源，但是授权记录未删除，而导致出现的脏数据，需要进行删除。
        permissionAuthorizationService.fixResourceAuthorization(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceAuthorizationIds = resourceAuthorizationIds
        )
    }

    private fun listResourceAuthorization(
        offset: Long,
        limit: Long,
        resourceType: String,
        projectCode: String
    ): ListResourcesAuthorizationDTO? {
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
                method = CallbackMethodEnum.LIST_RESOURCE_AUTHORIZATION
                filter = filterDTO
                page = PageInfoDTO().apply {
                    this.offset = offset
                    this.limit = limit
                }
            },
            token = tokenApi.getAccessToken(projectAuthServiceCode)
        ) as ListResourcesAuthorizationDTO?
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResourceAuthorizationService::class.java)
        private val executorService = Executors.newFixedThreadPool(10)
    }
}
