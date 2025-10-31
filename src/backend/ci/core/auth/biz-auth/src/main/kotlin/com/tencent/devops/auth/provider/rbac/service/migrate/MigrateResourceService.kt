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
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.dto.ResourceMigrationCountDTO
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.PermissionGradeManagerService
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.service.trace.TraceTag
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 将资源迁移到权限中心
 */
@Suppress("LongParameterList", "MagicNumber")
class MigrateResourceService @Autowired constructor(
    private val resourceService: ResourceService,
    private val rbacCommonService: RbacCommonService,
    private val rbacPermissionResourceService: RbacPermissionResourceService,
    private val migrateCreatorFixService: MigrateCreatorFixService,
    private val authResourceService: AuthResourceService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val tokenApi: AuthTokenApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceGroupDao: AuthResourceGroupDao
) {

    @Suppress("SpreadOperator")
    fun migrateResource(
        projectCode: String,
        projectCreator: String
    ) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource:$projectCode")
        try {
            val resourceTypes = rbacCommonService.listResourceTypes()
                .map { it.resourceType }
                .filterNot { noNeedToMigrateResourceType.contains(it) }

            logger.info("MigrateResourceService|resourceTypes:$resourceTypes")
            // 迁移各个资源类型下的资源
            val traceId = MDC.get(TraceTag.BIZID)
            val resourceTypeFuture = resourceTypes.map { resourceType ->
                CompletableFuture.supplyAsync(
                    {
                        MDC.put(TraceTag.BIZID, traceId)
                        migrateResource(
                            projectCode = projectCode,
                            resourceType = resourceType,
                            projectCreator = projectCreator
                        )
                    },
                    executorService
                )
            }
            CompletableFuture.allOf(*resourceTypeFuture.toTypedArray()).join()

            // 统计资源数
            val resourceCountDTOs = resourceTypes.map { resourceType ->
                calculateResourceCount(projectCode = projectCode, resourceType = resourceType)
            }
            authMigrationDao.updateResourceCount(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceCountInfo = JsonUtil.toJson(resourceCountDTOs)
            )
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource $projectCode")
        }
    }

    fun migrateResource(
        projectCode: String,
        resourceType: String,
        projectCreator: String,
        throwException: Boolean = true
    ) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource|$projectCode|$resourceType")
        try {
            createRbacResource(
                resourceType = resourceType,
                projectCode = projectCode,
                projectCreator = projectCreator,
                throwException = throwException
            )
        } catch (ignore: Exception) {
            logger.error("Failed to migrate resource|$projectCode|$resourceType", ignore)
            throw ignore
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource|$projectCode|$resourceType"
            )
        }
    }

    @Suppress("ALL")
    private fun createRbacResource(
        resourceType: String,
        projectCode: String,
        projectCreator: String,
        throwException: Boolean
    ) {
        var offset = 0L
        val limit = 100L
        do {
            val resourceData = listInstance(
                offset = offset,
                limit = limit,
                resourceType = resourceType,
                projectCode = projectCode
            )
            logger.info(
                "MigrateResourceService|projectCode:$projectCode|resourceType:$resourceType" +
                    "|resourceData:$resourceData"
            )
            if (resourceData == null || resourceData.data.result.isNullOrEmpty()) {
                return
            }
            val ids = resourceData.data.result.map { it.id }
            val instanceInfoList = fetchInstanceInfo(
                resourceType = resourceType,
                projectCode = projectCode,
                ids = ids
            ) ?: return
            instanceInfoList.data.map {
                JsonUtil.to(JsonUtil.toJson(it), InstanceInfoDTO::class.java)
            }.forEach { instance ->
                try {
                    val resourceCode =
                        migrateResourceCodeConverter.getRbacResourceCode(
                            projectCode = projectCode,
                            resourceType = resourceType,
                            migrateResourceCode = instance.id
                        ) ?: return@forEach
                    logger.info("MigrateResourceService|projectCode:$projectCode|resourceCode:$resourceCode")
                    authResourceService.getOrNull(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode
                    )?.let { authResource ->
                        // 如果存在,说明重复迁移,判断资源名称是否相同,如果不同则需要修改
                        if (instance.displayName != authResource.resourceName) {
                            rbacPermissionResourceService.resourceModifyRelation(
                                projectCode = projectCode,
                                resourceType = resourceType,
                                resourceCode = resourceCode,
                                resourceName = instance.displayName
                            )
                        }
                    } ?: run {
                        rbacPermissionResourceService.resourceCreateRelation(
                            userId = migrateCreatorFixService.getResourceCreator(
                                projectCreator = projectCreator,
                                resourceCreator = instance.iamApprover.first()
                            ),
                            projectCode = projectCode,
                            resourceType = resourceType,
                            resourceCode = resourceCode,
                            resourceName = instance.displayName,
                            async = false
                        )
                    }
                } catch (ex: Exception) {
                    if (throwException) {
                        throw ex
                    } else {
                        logger.warn("migrate resource failed . $projectCode|$resourceType|${instance.id}", ex)
                    }
                }
            }
            offset += limit
        } while (resourceData!!.data.result.size.toLong() == limit)
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

    fun fetchInstanceInfo(
        resourceType: String,
        projectCode: String,
        ids: List<String>
    ): FetchInstanceInfoResponseDTO? {
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
        ) as FetchInstanceInfoResponseDTO?
    }

    private fun calculateResourceCount(projectCode: String, resourceType: String): ResourceMigrationCountDTO {
        val count = authResourceService.countByProjectAndType(
            projectCode = projectCode,
            resourceType = resourceType
        )
        val groupConfigCount = authResourceGroupConfigDao.countByResourceType(
            dslContext = dslContext,
            resourceType = resourceType
        )
        return ResourceMigrationCountDTO(
            resourceType = resourceType,
            count = count,
            groupCount = count * groupConfigCount
        )
    }

    fun migrateProjectResource(
        projectCode: String,
        projectName: String,
        gradeManagerId: String,
        registerMonitorPermission: Boolean,
        migrateManagerGroup: Boolean,
        migrateOtherGroup: Boolean = true
    ) {
        logger.info("migrate project resources:$projectCode|$projectName|$gradeManagerId")
        if (migrateManagerGroup) {
            permissionGradeManagerService.modifyGradeManager(
                gradeManagerId = gradeManagerId,
                projectCode = projectCode,
                projectName = projectName,
                registerMonitorPermission = registerMonitorPermission
            )
        }
        if (migrateOtherGroup) {
            resetOtherProjectLevelGroupPermissions(
                projectCode = projectCode,
                projectName = projectName,
                registerMonitorPermission = registerMonitorPermission
            )
        }
    }

    fun resetOtherProjectLevelGroupPermissions(
        projectCode: String,
        projectName: String,
        registerMonitorPermission: Boolean,
        filterResourceTypes: List<String> = emptyList(),
        filterActions: List<String> = emptyList()
    ) {
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            createMode = false
        )
        defaultGroupConfigs.forEach { groupConfig ->
            val resourceGroupInfo = if (groupConfig.groupName == BkAuthGroup.CI_MANAGER.groupName) {
                authResourceGroupDao.getByGroupName(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    resourceCode = projectCode,
                    groupName = groupConfig.groupName
                )
            } else {
                authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    resourceCode = projectCode,
                    groupCode = groupConfig.groupCode
                )
            } ?: return@forEach
            // 项目下用户组注册监控权限资源
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = groupConfig.authorizationScopes,
                projectCode = projectCode,
                projectName = projectName,
                resourceType = groupConfig.resourceType,
                groupCode = groupConfig.groupCode,
                iamResourceCode = projectCode,
                resourceName = projectName,
                iamGroupId = resourceGroupInfo.relationId.toInt(),
                registerMonitorPermission = registerMonitorPermission,
                filterResourceTypes = filterResourceTypes,
                filterActions = filterActions
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResourceService::class.java)
        private val noNeedToMigrateResourceType = listOf(
            AuthResourceType.PROJECT.value
        )
        private val executorService = Executors.newFixedThreadPool(50)
    }
}
