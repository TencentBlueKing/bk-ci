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

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceSyncDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.lock.SyncGroupAndMemberLock
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors

@Suppress("LongParameterList")
class RbacPermissionResourceGroupSyncService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val authResourceService: AuthResourceService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val rbacCacheService: RbacCacheService,
    private val redisOperation: RedisOperation,
    private val authResourceSyncDao: AuthResourceSyncDao
) : PermissionResourceGroupSyncService {
    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupSyncService::class.java)
        private val syncExecutorService = Executors.newFixedThreadPool(5)
        private val syncProjectsExecutorService = Executors.newFixedThreadPool(10)
        private val syncResourceMemberExecutorService = Executors.newFixedThreadPool(50)
    }

    override fun syncByCondition(projectConditionDTO: ProjectConditionDTO) {
        logger.info("start to migrate project by condition|$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        syncExecutorService.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE / 2
            do {
                val projectCodes = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = projectConditionDTO,
                    limit = limit,
                    offset = offset
                ).data ?: break
                batchSyncGroupAndMember(projectCodes = projectCodes.map { it.englishName })
                offset += limit
            } while (projectCodes.size == limit)
        }
    }

    override fun batchSyncGroupAndMember(projectCodes: List<String>) {
        logger.info("sync all group and member|$projectCodes")
        if (projectCodes.isEmpty()) return
        projectCodes.forEach { projectCode ->
            syncGroupAndMember(projectCode = projectCode)
        }
    }

    override fun batchSyncProjectGroup(projectCodes: List<String>) {
        logger.info("sync all group|$projectCodes")
        if (projectCodes.isEmpty()) return
        val traceId = MDC.get(TraceTag.BIZID)
        projectCodes.forEach { projectCode ->
            syncProjectsExecutorService.submit {
                MDC.put(TraceTag.BIZID, traceId)
                syncProjectGroup(projectCode = projectCode)
            }
        }
    }

    override fun batchSyncAllMember(projectCodes: List<String>) {
        logger.info("sync all member|$projectCodes")
        if (projectCodes.isEmpty()) return
        val traceId = MDC.get(TraceTag.BIZID)
        projectCodes.forEach { projectCode ->
            syncProjectsExecutorService.submit {
                MDC.put(TraceTag.BIZID, traceId)
                syncResourceGroupMember(projectCode = projectCode)
            }
        }
    }

    override fun syncResourceMember(projectCode: String, resourceType: String, resourceCode: String) {
        logger.info("sync resource member|$projectCode|$resourceType|$resourceCode")
        val resourceGroups = authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        resourceGroups.forEach { resourceGroup ->
            syncResourceGroupMember(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = resourceGroup.groupCode,
                iamGroupId = resourceGroup.relationId.toInt()
            )
        }
    }

    override fun syncIamGroupMember(projectCode: String, iamGroupId: Int) {
        logger.info("sync resource member|$projectCode|$iamGroupId")
        val resourceGroup = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId.toString()
        ) ?: return
        syncResourceGroupMember(
            projectCode = projectCode,
            resourceType = resourceGroup.resourceType,
            resourceCode = resourceGroup.resourceCode,
            groupCode = resourceGroup.groupCode,
            iamGroupId = iamGroupId
        )
    }

    override fun syncGroupAndMember(projectCode: String) {
        val traceId = MDC.get(TraceTag.BIZID)
        syncProjectsExecutorService.submit {
            MDC.put(TraceTag.BIZID, traceId)
            SyncGroupAndMemberLock(redisOperation, projectCode).use { lock ->
                if (!lock.tryLock()) {
                    logger.info("sync group and member|running:$projectCode")
                    return@use
                }
                val startEpoch = System.currentTimeMillis()
                try {
                    logger.info("sync group and member|start:$projectCode")
                    authResourceSyncDao.create(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        status = AuthMigrateStatus.PENDING.value
                    )
                    // 同步项目下的组信息
                    syncProjectGroup(projectCode = projectCode)
                    // 同步组成员
                    syncResourceGroupMember(projectCode = projectCode)
                    // 记录完成状态
                    authResourceSyncDao.updateStatus(
                        dslContext = dslContext,
                        projectCode = projectCode,
                        status = AuthMigrateStatus.SUCCEED.value,
                        totalTime = System.currentTimeMillis() - startEpoch
                    )
                    logger.info(
                        "It take(${System.currentTimeMillis() - startEpoch})ms to sync " +
                            "project group and members $projectCode"
                    )
                } catch (ex: Exception) {
                    handleException(
                        exception = ex,
                        projectCode = projectCode,
                        totalTime = System.currentTimeMillis() - startEpoch
                    )
                }
            }
        }
    }

    override fun getStatusOfSync(projectCode: String): AuthMigrateStatus {
        val syncRecord = authResourceSyncDao.get(
            dslContext = dslContext,
            projectCode = projectCode
        ) ?: return AuthMigrateStatus.SUCCEED
        return AuthMigrateStatus.values().first { it.value == syncRecord.status }
    }

    private fun handleException(
        totalTime: Long,
        exception: Exception,
        projectCode: String
    ) {
        val errorMessage = when (exception) {
            is IamException -> {
                exception.errorMsg
            }
            is ErrorCodeException -> {
                exception.defaultMessage
            }
            is CompletionException -> {
                exception.cause?.message ?: exception.message
            }
            else -> {
                exception.toString()
            }
        }
        logger.warn("sync group and member error! $projectCode", errorMessage)
        authResourceSyncDao.updateStatus(
            dslContext = dslContext,
            projectCode = projectCode,
            status = AuthMigrateStatus.FAILED.value,
            errorMessage = errorMessage,
            totalTime = totalTime
        )
    }

    @Suppress("NestedBlockDepth")
    private fun syncProjectGroup(projectCode: String) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to sync project group :$projectCode")
        try {
            val projectInfo = authResourceService.get(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )

            val projectGroupMap = authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            ).associateBy { it.relationId.toInt() }

            // 查询项目下用户组列表
            val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
            val pageInfoDTO = V2PageInfoDTO().apply {
                page = 1
                pageSize = 1000
            }
            val iamGroupList = iamV2ManagerService.getGradeManagerRoleGroupV2(
                projectInfo.relationId,
                searchGroupDTO,
                pageInfoDTO
            ).results

            // 查询人员模板列表
            val templatePageInfoDTO = V2PageInfoDTO().apply {
                page = 1
                pageSize = 500
            }
            val templateMap = iamV2ManagerService.getGradeManagerRoleTemplate(
                projectInfo.relationId,
                null,
                templatePageInfoDTO
            ).results.associateBy { it.sourceGroupId }

            val iamGroupMap = iamGroupList.associateBy { it.id }
            val toDeleteGroups = projectGroupMap.filterKeys { !iamGroupMap.contains(it) }.values
            val toUpdateGroups = mutableListOf<AuthResourceGroup>()
            val toAddGroups = mutableListOf<AuthResourceGroup>()
            if (toDeleteGroups.isNotEmpty()) {
                logger.info("sync project group|delete group|${toDeleteGroups.map { it.groupName }}")
            }

            iamGroupList.forEach { iamGroupInfo ->
                val templateId = templateMap[iamGroupInfo.id]?.id
                if (projectGroupMap.contains(iamGroupInfo.id)) {
                    val projectGroup = projectGroupMap[iamGroupInfo.id]!!
                    // 用户组只有名称和描述可能会修改
                    if (projectGroup.groupName != iamGroupInfo.name ||
                        projectGroup.description != iamGroupInfo.description ||
                        projectGroup.iamTemplateId != templateId
                    ) {
                        toUpdateGroups.add(
                            authResourceGroupDao.convert(projectGroup).copy(
                                groupName = iamGroupInfo.name,
                                description = iamGroupInfo.description,
                                iamTemplateId = templateId
                            )
                        )
                    }
                } else {
                    toAddGroups.add(
                        AuthResourceGroup(
                            projectCode = projectCode,
                            resourceType = AuthResourceType.PROJECT.value,
                            resourceCode = projectCode,
                            resourceName = projectInfo.resourceName,
                            iamResourceCode = projectCode,
                            groupCode = DefaultGroupType.CUSTOM.value,
                            groupName = iamGroupInfo.name,
                            defaultGroup = false,
                            relationId = iamGroupInfo.id,
                            description = iamGroupInfo.description,
                            iamTemplateId = templateId
                        )
                    )
                }
            }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                authResourceGroupDao.deleteByIds(transactionContext, toDeleteGroups.map { it.id })
                authResourceGroupDao.batchCreate(transactionContext, toAddGroups)
                authResourceGroupDao.batchUpdate(transactionContext, toUpdateGroups)
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to sync project group $projectCode"
            )
        }
    }

    @Suppress("SpreadOperator")
    private fun syncResourceGroupMember(projectCode: String) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to sync resource group member:$projectCode")
        try {
            val resourceTypes = rbacCacheService.listResourceTypes().map { it.resourceType }
            val traceId = MDC.get(TraceTag.BIZID)
            val resourceTypeFuture = resourceTypes.map { resourceType ->
                CompletableFuture.supplyAsync(
                    {
                        MDC.put(TraceTag.BIZID, traceId)
                        syncResourceGroupMember(
                            projectCode = projectCode,
                            resourceType = resourceType
                        )
                    },
                    syncResourceMemberExecutorService
                )
            }
            CompletableFuture.allOf(*resourceTypeFuture.toTypedArray()).join()
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to sync resource group member $projectCode"
            )
        }
    }

    private fun syncResourceGroupMember(projectCode: String, resourceType: String) {
        val limit = 100
        var offset = 0
        val startEpoch = System.currentTimeMillis()
        logger.info("start to sync resource group member|$projectCode|$resourceType")
        try {
            do {
                val authResourceGroups = authResourceGroupDao.listGroupByResourceType(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    offset = offset,
                    limit = limit
                )
                authResourceGroups.forEach { authResourceGroup ->
                    try {
                        syncResourceGroupMember(
                            projectCode = projectCode,
                            resourceType = resourceType,
                            resourceCode = authResourceGroup.resourceCode,
                            groupCode = authResourceGroup.groupCode,
                            iamGroupId = authResourceGroup.relationId
                        )
                    } catch (ignore: Exception) {
                        logger.warn(
                            "sync resource group member failed!" +
                                "|$projectCode|${authResourceGroup.relationId}|$ignore"
                        )
                    }
                }
                offset += limit
            } while (authResourceGroups.size == limit)
        } catch (ignored: Exception) {
            logger.error("Failed to sync resource group member|$projectCode|$resourceType", ignored)
            throw ignored
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource|$projectCode|$resourceType"
            )
        }
    }

    private fun syncResourceGroupMember(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamGroupId: Int
    ) {
        val resourceGroupMembers = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            iamGroupId = iamGroupId
        )
        val toDeleteMembers = mutableListOf<AuthResourceGroupMember>()
        val toUpdateMembers = mutableListOf<AuthResourceGroupMember>()
        val toAddMembers = mutableListOf<AuthResourceGroupMember>()
        syncIamGroupMember(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = groupCode,
            iamGroupId = iamGroupId,
            resourceGroupMembers = resourceGroupMembers,
            toDeleteMembers = toDeleteMembers,
            toUpdateMembers = toUpdateMembers,
            toAddMembers = toAddMembers
        )
        syncIamGroupTemplate(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = groupCode,
            iamGroupId = iamGroupId,
            resourceGroupMembers = resourceGroupMembers,
            toDeleteMembers = toDeleteMembers,
            toUpdateMembers = toUpdateMembers,
            toAddMembers = toAddMembers
        )
        if (toDeleteMembers.isNotEmpty()) {
            logger.info("sync resource group member|delete group|${toDeleteMembers.map { it.memberId }}")
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            authResourceGroupMemberDao.batchDelete(transactionContext, toDeleteMembers.map { it.id!! }.toSet())
            authResourceGroupMemberDao.batchCreate(transactionContext, toAddMembers)
            authResourceGroupMemberDao.batchUpdate(transactionContext, toUpdateMembers)
        }
    }

    /**
     * 同步IAM用户组成员/组织
     */
    private fun syncIamGroupMember(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamGroupId: Int,
        resourceGroupMembers: List<AuthResourceGroupMember>,
        toDeleteMembers: MutableList<AuthResourceGroupMember>,
        toUpdateMembers: MutableList<AuthResourceGroupMember>,
        toAddMembers: MutableList<AuthResourceGroupMember>
    ) {
        val resourceGroupMemberMap = resourceGroupMembers.filter {
            it.memberType != ManagerScopesEnum.TEMPLATE.name
        }.associateBy { it.memberId }

        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        val iamGroupMemberList = iamV2ManagerService.getRoleGroupMemberV2(iamGroupId, pageInfoDTO).results
        val iamGroupMemberMap = iamGroupMemberList.associateBy { it.id }

        toDeleteMembers.addAll(resourceGroupMemberMap.filterKeys { !iamGroupMemberMap.contains(it) }.values)
        iamGroupMemberList.forEach { iamGroupMember ->
            val expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(iamGroupMember.expiredAt)
            if (resourceGroupMemberMap.contains(iamGroupMember.id)) {
                val resourceGroupMember = resourceGroupMemberMap[iamGroupMember.id]!!
                if (expiredTime != resourceGroupMember.expiredTime) {
                    toUpdateMembers.add(resourceGroupMember.copy(expiredTime = expiredTime))
                }
            } else {
                toAddMembers.add(
                    AuthResourceGroupMember(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        groupCode = groupCode,
                        iamGroupId = iamGroupId,
                        memberId = iamGroupMember.id,
                        memberName = iamGroupMember.name,
                        memberType = iamGroupMember.type,
                        expiredTime = expiredTime
                    )
                )
            }
        }
    }

    /**
     * 同步IAM用户组人员模板
     */
    private fun syncIamGroupTemplate(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamGroupId: Int,
        resourceGroupMembers: List<AuthResourceGroupMember>,
        toDeleteMembers: MutableList<AuthResourceGroupMember>,
        toUpdateMembers: MutableList<AuthResourceGroupMember>,
        toAddMembers: MutableList<AuthResourceGroupMember>
    ) {
        val resourceGroupMemberMap = resourceGroupMembers.filter {
            it.memberType == ManagerScopesEnum.TEMPLATE.name
        }.associateBy { it.memberId }

        val pageInfoDTO = V2PageInfoDTO().apply {
            pageSize = 1000
            page = 1
        }
        // 查询人员模板列表
        val iamGroupTemplateList = iamV2ManagerService.listRoleGroupTemplates(iamGroupId, pageInfoDTO).results
        val iamGroupTemplateMap = iamGroupTemplateList.associateBy { it.id }

        toDeleteMembers.addAll(resourceGroupMemberMap.filterKeys { !iamGroupTemplateMap.contains(it) }.values)
        iamGroupTemplateList.forEach { iamGroupTemplate ->
            val expiredTime = DateTimeUtil.convertTimestampToLocalDateTime(iamGroupTemplate.expiredAt)
            if (resourceGroupMemberMap.contains(iamGroupTemplate.id)) {
                val resourceGroupMember = resourceGroupMemberMap[iamGroupTemplate.id]!!
                if (expiredTime != resourceGroupMember.expiredTime) {
                    toUpdateMembers.add(resourceGroupMember.copy(expiredTime = expiredTime))
                }
            } else {
                toAddMembers.add(
                    AuthResourceGroupMember(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        groupCode = groupCode,
                        iamGroupId = iamGroupId,
                        memberId = iamGroupTemplate.id,
                        memberName = iamGroupTemplate.name,
                        memberType = ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE),
                        expiredTime = expiredTime
                    )
                )
            }
        }
    }
}
