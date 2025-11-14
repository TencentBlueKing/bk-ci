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

package com.tencent.devops.auth.provider.rbac.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.sdk.iam.dto.InstancesDTO
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.dao.AuthSyncDataTaskDao
import com.tencent.devops.auth.dao.AuthUserProjectPermissionDao
import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.auth.pojo.UserProjectPermission
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.AuthSyncDataType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthProjectLevelPermissionsSyncEvent
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.lock.SyncGroupPermissionLock
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServicePipelineViewResource
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("LongParameterList")
class RbacPermissionResourceGroupPermissionService(
    private val v2ManagerService: V2ManagerService,
    private val rbacCommonService: RbacCommonService,
    private val monitorSpaceService: AuthMonitorSpaceService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val dslContext: DSLContext,
    private val resourceGroupPermissionDao: AuthResourceGroupPermissionDao,
    private val converter: AuthResourceCodeConverter,
    private val client: Client,
    private val iamV2ManagerService: V2ManagerService,
    private val authAuthorizationScopesService: AuthAuthorizationScopesService,
    private val authActionDao: AuthActionDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val objectMapper: ObjectMapper,
    private val authResourceDao: AuthResourceDao,
    private val authUserProjectPermissionDao: AuthUserProjectPermissionDao,
    private val authResourceMemberDao: AuthResourceGroupMemberDao,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val syncDataTaskDao: AuthSyncDataTaskDao,
    private val redisOperation: RedisOperation
) : PermissionResourceGroupPermissionService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    @Value("\${monitor.register:false}")
    private val registerMonitor: Boolean = false

    @Value("\${monitor.iamSystem:}")
    private val monitorSystemId = ""

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceGroupPermissionService::class.java)
        private const val AUTH_RESOURCE_GROUP_PERMISSION_ID_TAG = "AUTH_RESOURCE_GROUP_PERMISSION_ID"
        private const val ALL_RESOURCE = "*"
        private val syncExecutorService = Executors.newFixedThreadPool(5)
        private val syncProjectsExecutorService = Executors.newFixedThreadPool(10)
        private val needToSyncProjectLevelAction = listOf(
            ActionId.PROJECT_VISIT, ActionId.PROJECT_MANAGE,
            ActionId.PIPELINE_TEMPLATE_CREATE, ActionId.PROJECT_VIEW
        )
    }

    private val projectCodeAndPipelineId2ViewIds = CacheHelper.createCache<String, List<String>>(duration = 10)

    override fun grantGroupPermission(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        groupCode: String,
        iamResourceCode: String,
        resourceName: String,
        iamGroupId: Int,
        registerMonitorPermission: Boolean,
        filterResourceTypes: List<String>,
        filterActions: List<String>
    ): Boolean {
        try {
            val authorizationScopes = mutableListOf<AuthorizationScopes>()
            val bkCiAuthorizationScopes = authAuthorizationScopesService.generateBkciAuthorizationScopes(
                authorizationScopesStr = authorizationScopesStr,
                projectCode = projectCode,
                projectName = projectName,
                iamResourceCode = iamResourceCode,
                resourceName = resourceName
            )
            // 若filterActions不为空，则本次新增的组权限，只和该action有关
            // 若filterResourceTypes不为空，则本次新增的组权限，只和该资源类型有关
            authorizationScopes.addAll(
                when {
                    filterActions.isNotEmpty() -> {
                        bkCiAuthorizationScopes.onEach { scope ->
                            scope.actions.retainAll { action ->
                                filterActions.contains(action.id)
                            }
                        }.filter { it.actions.isNotEmpty() }
                    }

                    filterResourceTypes.isNotEmpty() -> {
                        bkCiAuthorizationScopes.filter { scope ->
                            val resourceTypeOfScope = scope.resources.firstOrNull()?.type
                            when {
                                resourceTypeOfScope == ResourceTypeId.PROJECT -> {
                                    scope.actions.retainAll { action ->
                                        filterResourceTypes.contains(action.id.substringBeforeLast("_"))
                                    }
                                    scope.actions.isNotEmpty()
                                }

                                else -> filterResourceTypes.contains(resourceTypeOfScope)
                            }
                        }
                    }

                    else -> bkCiAuthorizationScopes
                }
            )
            if (resourceType == AuthResourceType.PROJECT.value && registerMonitorPermission) {
                // 若为项目下的组授权，默认要加上监控平台用户组的权限资源
                val monitorAuthorizationScopes = authAuthorizationScopesService.generateMonitorAuthorizationScopes(
                    projectName = projectName,
                    projectCode = projectCode,
                    groupCode = groupCode
                )
                authorizationScopes.addAll(monitorAuthorizationScopes)
            }
            logger.info(
                "grant group permissions authorization scopes :{}|{}|{}|{}",
                projectCode, iamGroupId, resourceType, JsonUtil.toJson(authorizationScopes)
            )
            authorizationScopes.forEach { authorizationScope ->
                RetryUtils.retry(3) {
                    iamV2ManagerService.grantRoleGroupV2(iamGroupId, authorizationScope)
                }
            }
        } finally {
            syncGroupPermissions(projectCode, iamGroupId)
        }
        return true
    }

    override fun grantAllProjectGroupsPermission(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ): Boolean {
        authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        ).forEach {
            val authorizationScopes = buildProjectPermissions(
                projectCode = projectCode,
                projectName = projectName,
                actions = actions
            )
            grantGroupPermission(
                authorizationScopesStr = authorizationScopes,
                projectCode = projectCode,
                projectName = projectName,
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = it.groupCode,
                iamResourceCode = projectCode,
                resourceName = projectName,
                iamGroupId = it.relationId,
                registerMonitorPermission = false
            )
        }
        return true
    }

    override fun buildProjectPermissions(
        projectCode: String,
        projectName: String,
        actions: List<String>
    ): String {
        val resourceType2Actions = actions.groupBy { it.substringBeforeLast("_") }
        val authorizationScopes = resourceType2Actions.map { (resourceType, actions) ->
            val projectPath = ManagerPath().apply {
                system = systemId
                id = projectCode
                name = projectName
                type = AuthResourceType.PROJECT.value
            }
            val resources = ManagerResources.builder()
                .system(systemId)
                .type(resourceType)
                .paths(listOf(listOf(projectPath)))
                .build()
            val iamActions = actions.map { Action(it) }
            AuthorizationScopes().also {
                it.resources = listOf(resources)
                it.actions = iamActions
                it.system = systemId
            }
        }
        return objectMapper.writeValueAsString(authorizationScopes)
    }

    override fun getGroupPolices(
        userId: String,
        projectCode: String,
        resourceType: String,
        iamGroupId: Int
    ): List<IamGroupPoliciesVo> {
        val groupInfo = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId.toString()
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST,
            params = arrayOf(iamGroupId.toString()),
            defaultMessage = "group $iamGroupId not exist"
        )
        val groupConfigInfo = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = groupInfo.groupCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf("${resourceType}_${groupInfo.groupCode}"),
            defaultMessage = "${resourceType}_${groupInfo.groupCode} group config  not exist"
        )
        val groupActions = JsonUtil.to(groupConfigInfo.actions, object : TypeReference<List<String>>() {})
        return authActionDao.list(
            dslContext = dslContext,
            resourceType = resourceType
        ).map {
            IamGroupPoliciesVo(
                action = it.action,
                actionName = it.actionName,
                permission = groupActions.contains(it.action)
            )
        }
    }

    override fun deleteByGroupIds(
        projectCode: String,
        iamGroupIds: List<Int>
    ): Boolean {
        resourceGroupPermissionDao.deleteByGroupIds(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = iamGroupIds
        )
        traceEventDispatcher.dispatch(
            AuthProjectLevelPermissionsSyncEvent(
                projectCode = projectCode,
                iamGroupIds = iamGroupIds
            )
        )
        return true
    }

    override fun listGroupsByPermissionConditions(
        projectCode: String,
        filterIamGroupIds: List<Int>?,
        relatedResourceType: String,
        relatedResourceCode: String?,
        action: String?
    ): List<Int> {
        val resourceType = if (action != null) {
            rbacCommonService.getActionInfo(action).relatedResourceType
        } else {
            relatedResourceType
        }
        val pipelineGroupIds = listPipelineGroupIds(
            projectCode = projectCode,
            resourceType = resourceType,
            relatedResourceCode = relatedResourceCode
        )
        return resourceGroupPermissionDao.listByConditions(
            dslContext = dslContext,
            projectCode = projectCode,
            filterIamGroupIds = filterIamGroupIds,
            resourceType = resourceType,
            resourceCode = relatedResourceCode,
            pipelineGroupIds = pipelineGroupIds,
            action = action
        )
    }

    override fun isGroupsHasPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        relatedResourceCode: String,
        action: String
    ): Boolean {
        if (filterIamGroupIds.isEmpty())
            return false
        if (relatedResourceType == ResourceTypeId.PROJECT) {
            return isGroupsHasProjectLevelPermission(
                projectCode = projectCode,
                filterIamGroupIds = filterIamGroupIds,
                action = action
            )
        }
        val resourceType = rbacCommonService.getActionInfo(action).relatedResourceType
        val pipelineGroupIds = listPipelineGroupIds(
            projectCode = projectCode,
            resourceType = resourceType,
            relatedResourceCode = relatedResourceCode
        )
        return resourceGroupPermissionDao.isGroupsHasPermission(
            dslContext = dslContext,
            projectCode = projectCode,
            filterIamGroupIds = filterIamGroupIds,
            resourceType = resourceType,
            resourceCode = relatedResourceCode,
            pipelineGroupIds = pipelineGroupIds,
            action = action
        )
    }

    override fun isGroupsHasProjectLevelPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        action: String
    ): Boolean {
        if (filterIamGroupIds.isEmpty())
            return false
        val actionRelatedResourceType = rbacCommonService.getActionInfo(action).relatedResourceType
        return resourceGroupPermissionDao.isGroupsHasProjectLevelPermission(
            dslContext = dslContext,
            projectCode = projectCode,
            filterIamGroupIds = filterIamGroupIds,
            actionRelatedResourceType = actionRelatedResourceType,
            action = action
        )
    }

    override fun listGroupResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): Map<String, List<String>> {
        if (filterIamGroupIds.isEmpty())
            return emptyMap()
        val resourceType = rbacCommonService.getActionInfo(action).relatedResourceType
        return resourceGroupPermissionDao.listGroupResourcesWithPermission(
            dslContext = dslContext,
            projectCode = projectCode,
            filterIamGroupIds = filterIamGroupIds,
            resourceType = resourceType,
            action = action
        )
    }

    override fun listResourcesWithPermission(
        projectCode: String,
        filterIamGroupIds: List<Int>,
        relatedResourceType: String,
        action: String
    ): List<String> {
        if (filterIamGroupIds.isEmpty())
            return emptyList()
        val resourceType = rbacCommonService.getActionInfo(action).relatedResourceType
        val resourceType2Resources = resourceGroupPermissionDao.listGroupResourcesWithPermission(
            dslContext = dslContext,
            projectCode = projectCode,
            filterIamGroupIds = filterIamGroupIds,
            resourceType = resourceType,
            action = action
        )

        return when {
            resourceType2Resources[ResourceTypeId.PROJECT] != null -> {
                authResourceDao.getResourceCodeByType(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            }

            resourceType == ResourceTypeId.PIPELINE -> {
                val authViewPipelineIds = resourceType2Resources[ResourceTypeId.PIPELINE_GROUP]?.let { authViewIds ->
                    client.get(ServicePipelineViewResource::class).listPipelineIdByViewIds(
                        projectId = projectCode,
                        viewIdsEncode = authViewIds
                    ).data
                } ?: emptyList()
                val pipelineIds = resourceType2Resources[ResourceTypeId.PIPELINE] ?: emptyList()
                (authViewPipelineIds + pipelineIds).toMutableSet().toList()
            }

            else -> {
                resourceType2Resources[resourceType] ?: emptyList()
            }
        }
    }

    private fun listPipelineGroupIds(
        projectCode: String,
        resourceType: String,
        relatedResourceCode: String?
    ): List<String> {
        return if (relatedResourceCode != null && resourceType == AuthResourceType.PIPELINE_DEFAULT.value) {
            CacheHelper.getOrLoad(projectCodeAndPipelineId2ViewIds, projectCode, relatedResourceCode) {
                client.get(ServicePipelineViewResource::class).listViewIdsByPipelineId(
                    projectId = projectCode,
                    pipelineId = relatedResourceCode
                ).data?.map { HashUtil.encodeLongId(it) } ?: emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun getGroupPermissionDetail(iamGroupId: Int): Map<String, List<GroupPermissionDetailVo>> {
        val groupPermissionMap = mutableMapOf<String, List<GroupPermissionDetailVo>>()
        groupPermissionMap[I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_DEVOPS_NAME)] =
            getGroupPermissionDetailBySystem(systemId, iamGroupId)
        if (registerMonitor) {
            val monitorGroupPermissionDetail = getGroupPermissionDetailBySystem(monitorSystemId, iamGroupId)
            if (monitorGroupPermissionDetail.isNotEmpty()) {
                groupPermissionMap[I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_MONITOR_NAME)] =
                    getGroupPermissionDetailBySystem(monitorSystemId, iamGroupId)
            }
        }
        return groupPermissionMap
    }

    override fun getGroupPermissionDetailBySystem(iamSystemId: String, iamGroupId: Int): List<GroupPermissionDetailVo> {
        val iamGroupPermissionDetailList = try {
            v2ManagerService.getGroupPermissionDetail(iamGroupId, iamSystemId)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_GROUP_PERMISSION_DETAIL_FAIL,
                params = arrayOf(iamGroupId.toString()),
                defaultMessage = "Failed to get group($iamGroupId) permission info"
            )
        }
        return iamGroupPermissionDetailList.map { detail ->
            val actionId = detail.id
            val relatedResourceTypesDTO = detail.resourceGroups[0].relatedResourceTypesDTO[0]
            val instances = relatedResourceTypesDTO.condition[0].instances
            val relatedResourceInfos = mutableListOf<RelatedResourceInfo>()
            instances.forEach { instance ->
                // 将resourceType转化为对应的资源类型名称
                buildRelatedResourceTypesName(
                    iamSystemId = iamSystemId,
                    instancesDTO = instance
                )
                relatedResourceInfos.add(
                    RelatedResourceInfo(
                        type = instance.type,
                        name = I18nUtil.getCodeLanMessage(
                            instance.type + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
                        ),
                        instance = instance.path
                    )
                )
            }
            val (actionName, actionRelatedResourceType) = if (iamSystemId == monitorSystemId) {
                Pair(monitorSpaceService.getMonitorActionName(action = actionId), monitorSystemId)
            } else {
                val actionInfo = rbacCommonService.getActionInfo(action = actionId)
                Pair(actionInfo.actionName, actionInfo.relatedResourceType)
            }
            GroupPermissionDetailVo(
                actionId = actionId,
                name = actionName!!,
                actionRelatedResourceType = actionRelatedResourceType,
                relatedResourceInfos = relatedResourceInfos
            )
        }.sortedBy { it.actionId }
    }

    private fun buildRelatedResourceTypesName(iamSystemId: String, instancesDTO: InstancesDTO) {
        instancesDTO.let {
            val resourceTypeName = if (iamSystemId == systemId) {
                rbacCommonService.getResourceTypeInfo(it.type).name
            } else {
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_MONITOR_SPACE)
            }
            it.name = resourceTypeName
            it.path.forEach { element1 ->
                element1.forEach { element2 ->
                    element2.typeName = resourceTypeName
                }
            }
        }
    }

    override fun syncGroupPermissions(projectCode: String, iamGroupId: Int): Boolean {
        return try {
            val resourceGroupInfo = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                relationId = iamGroupId.toString()
            ) ?: return true
            val groupPermissionDetails = getGroupPermissionDetailBySystem(systemId, iamGroupId)
            logger.info("sync group permissions: {}|{}|{}", projectCode, iamGroupId, groupPermissionDetails)

            val latestResourceGroupPermissions = groupPermissionDetails.flatMap { permissionDetail ->
                permissionDetail.relatedResourceInfos.flatMap { relatedResourceInfo ->
                    relatedResourceInfo.instance.mapNotNull { instancePathDTOs ->
                        try {
                            val (relatedResourceType, relatedResourceCode, relatedIamResourceCode) = when {
                                // 带*号，则为项目下所有资源。
                                instancePathDTOs.size > 1 && instancePathDTOs.last().id == ALL_RESOURCE -> {
                                    Triple(AuthResourceType.PROJECT.value, projectCode, projectCode)
                                }

                                else -> {
                                    val relatedIamResourceCode = converter.iamCode2Code(
                                        projectCode = projectCode,
                                        resourceType = instancePathDTOs.last().type,
                                        iamResourceCode = instancePathDTOs.last().id
                                    )
                                    Triple(
                                        first = instancePathDTOs.last().type,
                                        second = relatedIamResourceCode,
                                        third = instancePathDTOs.last().id
                                    )
                                }
                            }
                            ResourceGroupPermissionDTO(
                                projectCode = projectCode,
                                resourceType = resourceGroupInfo.resourceType,
                                resourceCode = resourceGroupInfo.resourceCode,
                                iamResourceCode = resourceGroupInfo.iamResourceCode,
                                groupCode = resourceGroupInfo.groupCode,
                                iamGroupId = iamGroupId,
                                action = permissionDetail.actionId,
                                actionRelatedResourceType = permissionDetail.actionRelatedResourceType,
                                relatedResourceType = relatedResourceType,
                                relatedResourceCode = relatedResourceCode,
                                relatedIamResourceCode = relatedIamResourceCode
                            )
                        } catch (ex: Exception) {
                            logger.warn("convert iam code to resource code failed!|${ex.message}")
                            null
                        }
                    }
                }
            }.distinct()

            val oldResourceGroupPermissions = resourceGroupPermissionDao.listByGroupId(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupId = iamGroupId
            )

            val toDeleteRecords = oldResourceGroupPermissions.filter { it !in latestResourceGroupPermissions }
            val toAddRecords = latestResourceGroupPermissions.filter { it !in oldResourceGroupPermissions }.map {
                it.copy(
                    id = client.get(ServiceAllocIdResource::class).generateSegmentId(
                        bizTag = AUTH_RESOURCE_GROUP_PERMISSION_ID_TAG
                    ).data!!
                )
            }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                if (toDeleteRecords.isNotEmpty()) {
                    resourceGroupPermissionDao.batchDeleteByIds(
                        dslContext = transactionContext,
                        projectCode = projectCode,
                        ids = toDeleteRecords.map { it.id!! })
                }
                resourceGroupPermissionDao.batchCreate(
                    dslContext = transactionContext,
                    records = toAddRecords
                )
            }
            traceEventDispatcher.dispatch(
                AuthProjectLevelPermissionsSyncEvent(
                    projectCode = projectCode,
                    iamGroupIds = listOf(iamGroupId),
                )
            )
            true
        } catch (ex: Exception) {
            logger.warn("sync group permissions failed! $projectCode|$iamGroupId|$ex")
            false
        }
    }

    override fun syncProjectPermissions(projectCode: String): Boolean {
        SyncGroupPermissionLock(redisOperation, projectCode).use { lock ->
            if (!lock.tryLock()) {
                logger.info("sync group permissions|running:$projectCode")
                return@use
            }
            logger.info("sync project group permissions:$projectCode")
            val iamGroupIds = authResourceGroupDao.listIamGroupIdsByConditions(
                dslContext = dslContext,
                projectCode = projectCode
            )
            logger.debug("sync project group permissions iamGroupIds:{}", iamGroupIds)
            iamGroupIds.forEach {
                syncGroupPermissions(
                    projectCode = projectCode,
                    iamGroupId = it
                )
            }
            val groupsWithPermissions = resourceGroupPermissionDao.listGroupsWithPermissions(
                dslContext = dslContext,
                projectCode = projectCode
            )
            val toDeleteGroupIds = groupsWithPermissions.filter { it !in iamGroupIds }
            deleteByGroupIds(
                projectCode = projectCode,
                iamGroupIds = toDeleteGroupIds
            )
        }
        return true
    }

    override fun syncPermissionsByCondition(projectConditionDTO: ProjectConditionDTO): Boolean {
        logger.info("start to sync group permissions by condition by condition|$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        syncExecutorService.submit {
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE / 2
            val uuid = UUIDUtil.generate()
            syncDataTaskDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = uuid,
                taskType = AuthSyncDataType.GROUP_PERMISSIONS_SYNC_TASK_TYPE.type
            )
            val result = mutableListOf<CompletableFuture<*>>()
            do {
                val projectCodes = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = projectConditionDTO,
                    limit = limit,
                    offset = offset
                ).data ?: break
                projectCodes.forEach {
                    result.add(
                        CompletableFuture.supplyAsync(
                            {
                                MDC.put(TraceTag.BIZID, traceId)
                                syncProjectPermissions(it.englishName)
                            },
                            syncProjectsExecutorService
                        )
                    )
                }
                offset += limit
            } while (projectCodes.size == limit)
            CompletableFuture.allOf(*result.toTypedArray()).join()
            syncDataTaskDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = uuid,
                taskType = AuthSyncDataType.GROUP_PERMISSIONS_SYNC_TASK_TYPE.type
            )
        }
        return true
    }

    override fun deleteByResource(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            resourceGroupPermissionDao.deleteByResourceCode(
                dslContext = transactionContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            resourceGroupPermissionDao.deleteByRelatedResourceCode(
                dslContext = transactionContext,
                projectCode = projectCode,
                relatedResourceType = resourceType,
                relatedResourceCode = resourceCode
            )
        }
        val iamGroupIds = authResourceGroupDao.listIamGroupIdsByConditions(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        traceEventDispatcher.dispatch(
            AuthProjectLevelPermissionsSyncEvent(
                projectCode = projectCode,
                iamGroupIds = iamGroupIds
            )
        )
        return true
    }

    override fun syncProjectLevelPermissions(projectCode: String): Boolean {
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE
        do {
            val groupIds = authResourceGroupDao.listIamGroupIdsByConditions(
                dslContext = dslContext,
                projectCode = projectCode,
                limit = limit,
                offset = offset
            )
            groupIds.forEach {
                syncProjectLevelPermissions(
                    projectCode = projectCode,
                    iamGroupId = it
                )
            }
            offset += limit
        } while (groupIds.size == limit)
        return true
    }

    private fun buildUserProjectPermission(projectCode: String, iamGroupId: Int): List<UserProjectPermission> {
        val actions = resourceGroupPermissionDao.getProjectLevelPermission(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId
        ).filter { needToSyncProjectLevelAction.contains(it) }

        if (actions.isEmpty()) {
            return emptyList()
        }

        val members = authResourceMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId,
            minExpiredTime = LocalDateTime.now()
        ).filterNot { it.memberType == MemberType.TEMPLATE.type }

        return members.flatMap { member ->
            actions.map { action ->
                UserProjectPermission(
                    memberId = member.memberId,
                    projectCode = projectCode,
                    action = action,
                    iamGroupId = iamGroupId,
                    expireTime = member.expiredTime
                )
            }
        }
    }

    override fun syncProjectLevelPermissions(
        projectCode: String,
        iamGroupId: Int
    ): Boolean {
        logger.info("start to sync group project level permissions,{}|{}", projectCode, iamGroupId)
        val startEpoch = System.currentTimeMillis()
        try {
            val authResourceGroup = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                relationId = iamGroupId.toString()
            )
            if (authResourceGroup == null) {
                logger.info("delete group project level permissions,{}|{}", projectCode, iamGroupId)
                authUserProjectPermissionDao.delete(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    iamGroupId = iamGroupId
                )
                return true
            }
            logger.info("sync project level permissions,{}|{}", projectCode, iamGroupId)
            val oldRecords = authUserProjectPermissionDao.list(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupId = iamGroupId
            )
            val lastedRecords = buildUserProjectPermission(
                projectCode = projectCode,
                iamGroupId = iamGroupId
            )

            val toDeleteRecords = oldRecords.filter { it !in lastedRecords }
            val toAddRecords = lastedRecords.filter { it !in oldRecords }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                authUserProjectPermissionDao.delete(
                    dslContext = transactionContext,
                    projectCode = projectCode,
                    iamGroupId = iamGroupId,
                    records = toDeleteRecords
                )
                authUserProjectPermissionDao.create(
                    dslContext = transactionContext,
                    records = toAddRecords
                )
            }
        } catch (ex: Exception) {
            logger.warn("sync group project level permissions failed ", ex)
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms " +
                    "sync group project level permissions $projectCode|$iamGroupId"
            )
        }

        return true
    }

    override fun syncProjectLevelPermissionsByCondition(projectConditionDTO: ProjectConditionDTO): Boolean {
        logger.info("start to sync user project permissions by condition by condition|$projectConditionDTO")
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
                projectCodes.forEach {
                    syncProjectsExecutorService.submit {
                        syncProjectLevelPermissions(it.englishName)
                    }
                }
                offset += limit
            } while (projectCodes.size == limit)
        }
        return true
    }

    override fun listProjectsWithPermission(
        memberIds: List<String>,
        action: String
    ): List<String> {
        return authUserProjectPermissionDao.list(
            dslContext = dslContext,
            memberIds = memberIds,
            action = action
        )
    }
}
