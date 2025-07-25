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

import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.dao.AuthSyncDataTaskDao
import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.auth.pojo.enum.AuthSyncDataType
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.PermissionGradeManagerService
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors

/**
 * rbac迁移服务
 */
@Suppress("LongParameterList", "ReturnCount")
class RbacPermissionMigrateService(
    private val client: Client,
    private val migrateResourceService: MigrateResourceService,
    private val migrateV3PolicyService: MigrateV3PolicyService,
    private val migrateV0PolicyService: MigrateV0PolicyService,
    private val migrateResultService: MigrateResultService,
    private val permissionResourceService: PermissionResourceService,
    private val authResourceService: AuthResourceService,
    private val migrateCreatorFixService: MigrateCreatorFixService,
    private val migratePermissionHandoverService: MigratePermissionHandoverService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao,
    private val authMonitorSpaceDao: AuthMonitorSpaceDao,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val migrateResourceAuthorizationService: MigrateResourceAuthorizationService,
    private val migrateResourceGroupService: MigrateResourceGroupService,
    private val syncDataTaskDao: AuthSyncDataTaskDao,
    private val rbacCommonService: RbacCommonService
) : PermissionMigrateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionMigrateService::class.java)
        private const val ALL_MEMBERS = "*"
        private const val ALL_MEMBERS_NAME = "allMembersName"
        private val toRbacExecutorService = Executors.newFixedThreadPool(5)
        private val migrateProjectsExecutorService = Executors.newFixedThreadPool(5)
    }

    @Value("\${auth.migrateProjectTag:#{null}}")
    private val migrateProjectTag: String = ""

    override fun v3ToRbacAuth(projectCodes: List<String>): Boolean {
        logger.info("migrate $projectCodes auth from v3 to rbac")
        if (projectCodes.isEmpty()) return true
        val projectVos =
            client.get(ServiceProjectResource::class).listByProjectCode(
                projectCodes = projectCodes.toSet()
            ).data ?: run {
                logger.info("migrate project info is empty")
                return false
            }
        val migrateProjectRelationIds = projectVos.filter { !it.relationId.isNullOrBlank() }.map { it.relationId!! }
        // 1. 启动迁移任务
        migrateV3PolicyService.startMigrateTask(
            v3GradeManagerIds = migrateProjectRelationIds
        )
        val traceId = MDC.get(TraceTag.BIZID)
        projectCodes.forEach { projectCode ->
            migrateProjectsExecutorService.submit {
                MDC.put(TraceTag.BIZID, traceId)
                migrateToRbacAuth(
                    projectCode = projectCode,
                    migrateTaskId = 0,
                    authType = AuthSystemType.V3_AUTH_TYPE
                )
            }
        }
        return true
    }

    override fun v0ToRbacAuth(projectCodes: List<String>): Boolean {
        logger.info("migrate $projectCodes auth from v0 to rbac")
        if (projectCodes.isEmpty()) return true
        // 1. 启动迁移任务
        val migrateTaskId = migrateV0PolicyService.startMigrateTask(
            projectCodes = projectCodes
        )
        val traceId = MDC.get(TraceTag.BIZID)
        projectCodes.forEach { projectCode ->
            migrateProjectsExecutorService.submit {
                MDC.put(TraceTag.BIZID, traceId)
                migrateToRbacAuth(
                    projectCode = projectCode,
                    migrateTaskId = migrateTaskId,
                    authType = AuthSystemType.V0_AUTH_TYPE
                )
            }
        }
        return true
    }

    override fun allToRbacAuth(): Boolean {
        logger.info("start to migrate all project")
        toRbacAuthByCondition(ProjectConditionDTO())
        return true
    }

    override fun toRbacAuthByCondition(
        projectConditionDTO: ProjectConditionDTO
    ): Boolean {
        logger.info("start to migrate project by condition|$projectConditionDTO")
        val traceId = MDC.get(TraceTag.BIZID)
        toRbacExecutorService.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE / 2
            do {
                val migrateProjects = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = projectConditionDTO,
                    limit = limit,
                    offset = offset
                ).data ?: break
                // 1.获取v0、v3项目
                val v3MigrateProjectCodes =
                    migrateProjects.filter {
                        it.routerTag == null || it.routerTag == AuthSystemType.V3_AUTH_TYPE.value
                    }.map { it.englishName }
                logger.info("migrate project to rbac|v3MigrateProjects:$v3MigrateProjectCodes")
                val v0MigrateProjectCodes =
                    migrateProjects.filter { it.routerTag == AuthSystemType.V0_AUTH_TYPE.value }
                        .map { it.englishName }
                logger.info("migrate project to rbac|v0MigrateProjects:$v0MigrateProjectCodes")
                // 2.迁移项目
                v3ToRbacAuth(projectCodes = v3MigrateProjectCodes)
                v0ToRbacAuth(projectCodes = v0MigrateProjectCodes)
                offset += limit
            } while (migrateProjects.size == limit)
        }
        return true
    }

    override fun compareResult(projectCode: String): Boolean {
        try {
            migrateResultService.compare(projectCode)
        } catch (ignored: Exception) {
            handleException(
                exception = ignored,
                projectCode = projectCode
            )
            return false
        }
        return true
    }

    // 包含修改分级管理员范围/重置项目级别默认组权限/迁移某类资源并创建用户组
    @Suppress("NestedBlockDepth")
    private fun resetProjectPermissions(
        projectCode: String,
        migrateResource: Boolean,
        filterResourceTypes: List<String> = emptyList(),
        filterActions: List<String> = emptyList()
    ) {
        logger.info(
            "reset project permissions {}|{}|{}|{}",
            projectCode, migrateResource, filterResourceTypes, filterActions
        )
        try {
            val projectInfo = authResourceService.get(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )
            val projectDetails = client.get(ServiceProjectResource::class).get(projectCode).data!!
            // 修改分级管理员范围
            permissionGradeManagerService.modifyGradeManager(
                gradeManagerId = projectInfo.relationId,
                projectCode = projectCode,
                projectName = projectDetails.projectName,
                registerMonitorPermission = true
            )
            // 重置项目级用户组权限
            migrateResourceService.resetOtherProjectLevelGroupPermissions(
                projectCode = projectCode,
                projectName = projectInfo.resourceName,
                registerMonitorPermission = false,
                filterResourceTypes = filterResourceTypes,
                filterActions = filterActions
            )
            // 迁移资源，若资源从未迁移过，则进行注册。迁移过，将重置资源下用户组的权限
            if (migrateResource && filterResourceTypes.isNotEmpty()) {
                filterResourceTypes.forEach {
                    migrateResourceService.migrateResource(
                        projectCode = projectCode,
                        resourceType = it,
                        projectCreator = migrateCreatorFixService.getProjectCreator(
                            projectCode = projectCode,
                            authSystemType = AuthSystemType.V0_AUTH_TYPE,
                            projectCreator = projectDetails.creator!!,
                            projectUpdator = projectDetails.updator
                        )!!
                    )
                    // 若迁移流水线模板权限，需要修改项目的properties字段
                    if (it == ResourceTypeId.PIPELINE_TEMPLATE) {
                        val properties = projectDetails.properties ?: ProjectProperties()
                        properties.enableTemplatePermissionManage = true
                        logger.info("update project(${projectDetails.englishName}) properties|$properties")
                        client.get(ServiceProjectResource::class).updateProjectProperties(
                            projectDetails.englishName,
                            properties
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            logger.warn("reset project permissions failed :$projectCode|$ex")
        }
    }

    override fun resetProjectPermissions(migrateResourceDTO: MigrateResourceDTO): Boolean {
        logger.info("reset project permissions by conditions {}", migrateResourceDTO)
        toRbacExecutorService.execute {
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            var count = 0
            val uuid = UUIDUtil.generate()
            syncDataTaskDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = uuid,
                taskType = AuthSyncDataType.PROJECT_PERMISSIONS_RESET_TASK_TYPE.type
            )
            val result = mutableListOf<CompletableFuture<*>>()
            val traceId = MDC.get(TraceTag.BIZID)
            do {
                val migrateProjects = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = migrateResourceDTO.conditions,
                    limit = limit,
                    offset = offset
                ).data ?: break
                migrateProjects.forEach {
                    result.add(
                        CompletableFuture.supplyAsync(
                            {
                                MDC.put(TraceTag.BIZID, traceId)
                                resetProjectPermissions(
                                    projectCode = it.englishName,
                                    migrateResource = migrateResourceDTO.migrateResource,
                                    filterResourceTypes = migrateResourceDTO.filterResourceTypes,
                                    filterActions = migrateResourceDTO.filterActions
                                )
                            },
                            migrateProjectsExecutorService
                        )
                    )
                }
                offset += limit
                count += migrateProjects.size
            } while (migrateProjects.size == limit)
            CompletableFuture.allOf(*result.toTypedArray()).join()
            syncDataTaskDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = uuid,
                taskType = AuthSyncDataType.PROJECT_PERMISSIONS_RESET_TASK_TYPE.type
            )
            logger.info("migrate specific resource of all projects successfully :$count")
        }
        return true
    }

    override fun resetPermissionsWhenEnabledProject(projectCode: String): Boolean {
        logger.info("reset permissions when enabled project:{}", projectCode)
        resetProjectPermissions(
            projectCode = projectCode,
            migrateResource = true,
            filterResourceTypes = rbacCommonService.listResourceTypes()
                .map { it.resourceType }.filterNot { it == ResourceTypeId.PROJECT },
            filterActions = emptyList()
        )
        return true
    }

    override fun migrateMonitorResource(
        projectCodes: List<String>,
        asyncMigrateManagerGroup: Boolean,
        asyncMigrateOtherGroup: Boolean
    ): Boolean {
        val traceId = MDC.get(TraceTag.BIZID)
        client.get(ServiceProjectResource::class).listByProjectCode(
            projectCodes = projectCodes.toSet()
        ).data?.forEach {
            // 若已迁移监控资源，直接跳过
            if (authMonitorSpaceDao.get(dslContext, it.englishName) != null)
                return@forEach
            val projectInfo = authResourceService.get(
                projectCode = it.englishName,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = it.englishName
            )
            if (!asyncMigrateManagerGroup) {
                permissionGradeManagerService.modifyGradeManager(
                    gradeManagerId = projectInfo.relationId,
                    projectCode = it.englishName,
                    projectName = projectInfo.resourceName,
                    registerMonitorPermission = true
                )
            }
            if (!asyncMigrateOtherGroup) {
                migrateResourceService.resetOtherProjectLevelGroupPermissions(
                    projectCode = projectInfo.projectCode,
                    projectName = projectInfo.resourceName,
                    registerMonitorPermission = true
                )
            }
            migrateProjectsExecutorService.submit {
                MDC.put(TraceTag.BIZID, traceId)
                migrateResourceService.migrateProjectResource(
                    projectCode = it.englishName,
                    projectName = projectInfo.resourceName,
                    gradeManagerId = projectInfo.relationId,
                    registerMonitorPermission = true,
                    migrateManagerGroup = asyncMigrateManagerGroup,
                    migrateOtherGroup = asyncMigrateOtherGroup
                )
            }
        }
        return true
    }

    override fun grantGroupAdditionalAuthorization(projectCodes: List<String>): Boolean {
        logger.info("grant group additional authorization|projectCode:$projectCodes")
        projectCodes.forEach { migrateV0PolicyService.grantGroupAdditionalAuthorization(projectCode = it) }
        return true
    }

    override fun handoverAllPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean {
        logger.info("handover all permissions :$permissionHandoverDTO")
        toRbacExecutorService.submit {
            migratePermissionHandoverService.handoverAllPermissions(permissionHandoverDTO = permissionHandoverDTO)
        }
        return true
    }

    override fun handoverPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean {
        logger.info("handover permissions :$permissionHandoverDTO")
        toRbacExecutorService.submit {
            migratePermissionHandoverService.handoverPermissions(permissionHandoverDTO = permissionHandoverDTO)
        }
        return true
    }

    @Suppress("LongMethod", "ReturnCount", "ComplexMethod")
    private fun migrateToRbacAuth(
        projectCode: String,
        migrateTaskId: Int,
        authType: AuthSystemType
    ): Boolean {
        logger.info("Start migrate $projectCode from $authType to rbac")
        val startEpoch = System.currentTimeMillis()
        val watcher = Watcher("migrateToRbacAuth|$projectCode")
        try {
            val authMigrationInfo = authMigrationDao.get(
                dslContext = dslContext,
                projectCode = projectCode
            )
            if (authMigrationInfo != null && authMigrationInfo.status == AuthMigrateStatus.PENDING.value) {
                logger.info("project $projectCode is migrating")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.ERROR_PROJECT_IN_MIGRATION,
                    defaultMessage = "project $projectCode is migrating"
                )
            }
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data
                ?: run {
                    logger.warn("project $projectCode not exist")
                    throw ErrorCodeException(
                        errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
                        defaultMessage = "project not exist $projectCode"
                    )
                }
            authMigrationDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                status = AuthMigrateStatus.PENDING.value,
                routerTag = authType.value
            )
            val projectCreator = migrateCreatorFixService.getProjectCreator(
                projectCode = projectCode,
                authSystemType = authType,
                projectCreator = projectInfo.creator!!,
                projectUpdator = projectInfo.updator
            ) ?: run {
                logger.warn("project($projectCode) creator(${projectInfo.creator}) not exist")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.ERROR_CREATOR_NOT_EXIST,
                    defaultMessage = "project($projectCode) creator(${projectInfo.creator}) not exist"
                )
            }
            logger.info("project creator is $projectCreator")
            // 创建分级管理员
            watcher.start("createGradeManager")
            val gradeManagerId = authResourceService.getOrNull(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )?.relationId?.toInt() ?: run {
                createGradeManager(
                    projectCode = projectCode,
                    projectInfo = projectInfo,
                    projectCreator = projectCreator
                )
            } ?: run {
                logger.warn("project $projectCode gradle manager not found")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.CAN_NOT_FIND_RELATION,
                    defaultMessage = "project $projectCode gradle manager not found"
                )
            }
            // 迁移资源
            watcher.start("migrateResource")
            migrateResourceService.migrateResource(
                projectCode = projectCode,
                projectCreator = projectCreator
            )

            when (authType) {
                AuthSystemType.V0_AUTH_TYPE -> {
                    migrateV0Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        migrateTaskId = migrateTaskId,
                        gradeManagerId = gradeManagerId,
                        version = authType.value,
                        watcher = watcher
                    )
                }

                AuthSystemType.V3_AUTH_TYPE -> {
                    migrateV3Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        gradeManagerId = gradeManagerId,
                        version = authType.value,
                        watcher = watcher
                    )
                }

                else -> {}
            }
            // 修改项目可授权人员范围
            if (projectInfo.subjectScopes == null || projectInfo.subjectScopes!!.isEmpty()) {
                client.get(ServiceProjectResource::class).updateProjectSubjectScopes(
                    projectId = projectCode,
                    subjectScopes = listOf(
                        SubjectScopeInfo(
                            id = ALL_MEMBERS,
                            type = ALL_MEMBERS,
                            name = I18nUtil.getCodeLanMessage(ALL_MEMBERS_NAME)
                        )
                    )
                )
            }
            // 设置项目路由tag
            if (migrateProjectTag.isNotBlank()) {
                client.get(ServiceProjectTagResource::class).updateProjectRouteTag(
                    projectCode = projectCode,
                    tag = migrateProjectTag
                )
            }
            authMigrationDao.updateStatus(
                dslContext = dslContext,
                projectCode = projectCode,
                status = AuthMigrateStatus.SUCCEED.value,
                totalTime = System.currentTimeMillis() - startEpoch
            )
            return true
        } catch (ignored: Exception) {
            handleException(
                exception = ignored,
                projectCode = projectCode
            )
            return false
        } finally {
            watcher.stop()
            logger.info("watcher migrate $projectCode|$watcher")
        }
    }

    private fun migrateV3Auth(
        projectCode: String,
        projectName: String,
        version: String,
        gradeManagerId: Int,
        watcher: Watcher
    ) {
        // 轮询任务状态
        migrateV3PolicyService.loopTaskStatus(projectCode = projectCode)
        // 迁移v3用户组
        watcher.start("migrateGroupPolicy")
        migrateV3PolicyService.migrateGroupPolicy(
            projectCode = projectCode,
            projectName = projectName,
            version = version,
            gradeManagerId = gradeManagerId
        )
        // 迁移用户自定义权限
        watcher.start("migrateUserCustomPolicy")
        migrateV3PolicyService.migrateUserCustomPolicy(
            projectCode = projectCode,
            projectName = projectName,
            version = version,
            gradeManagerId = gradeManagerId
        )
        // 对比迁移结果
        watcher.start("comparePolicy")
        migrateResultService.compare(projectCode = projectCode)
    }

    private fun migrateV0Auth(
        projectCode: String,
        projectName: String,
        migrateTaskId: Int,
        gradeManagerId: Int,
        version: String,
        watcher: Watcher
    ) {
        // 轮询任务状态
        migrateV0PolicyService.loopTaskStatus(migrateTaskId = migrateTaskId)
        // 迁移v0用户组
        watcher.start("migrateGroupPolicy")
        migrateV0PolicyService.migrateGroupPolicy(
            projectCode = projectCode,
            projectName = projectName,
            version = version,
            gradeManagerId = gradeManagerId
        )
        // 迁移用户自定义权限
        watcher.start("migrateUserCustomPolicy")
        migrateV0PolicyService.migrateUserCustomPolicy(
            projectCode = projectCode,
            projectName = projectName,
            version = version,
            gradeManagerId = gradeManagerId
        )
        // 对比迁移结果
        watcher.start("comparePolicy")
        migrateResultService.compare(projectCode = projectCode)
    }

    private fun createGradeManager(
        projectCode: String,
        projectInfo: ProjectVO,
        projectCreator: String
    ): Int? {
        client.get(ServiceProjectApprovalResource::class).createMigration(projectId = projectCode)
        permissionResourceService.resourceCreateRelation(
            userId = projectCreator,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectInfo.projectName,
            async = false
        )
        return authResourceService.getOrNull(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )?.relationId?.toInt()
    }

    private fun handleException(exception: Exception, projectCode: String) {
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
        logger.warn("Failed to migrate $projectCode", exception)
        authMigrationDao.updateStatus(
            dslContext = dslContext,
            projectCode = projectCode,
            status = AuthMigrateStatus.FAILED.value,
            errorMessage = errorMessage,
            totalTime = null
        )
    }

    override fun autoRenewal(
        validExpiredDay: Int,
        projectConditionDTO: ProjectConditionDTO
    ): Boolean {
        val traceId = MDC.get(TraceTag.BIZID)
        toRbacExecutorService.submit {
            MDC.put(TraceTag.BIZID, traceId)
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE / 2
            do {
                val migrateProjects = client.get(ServiceProjectResource::class).listProjectsByCondition(
                    projectConditionDTO = projectConditionDTO.copy(
                        enabled = true
                    ),
                    limit = limit,
                    offset = offset
                ).data ?: break
                migrateProjects.forEach { migrateProject ->
                    migrateProjectsExecutorService.submit {
                        MDC.put(TraceTag.BIZID, traceId)
                        autoRenewal(
                            projectCode = migrateProject.englishName,
                            validExpiredDay = validExpiredDay
                        )
                    }
                }
                offset += limit
            } while (migrateProjects.size == limit)
        }
        return true
    }

    private fun autoRenewal(
        projectCode: String,
        validExpiredDay: Int
    ) {
        var offset = 0
        val limit = 100
        val startTime = System.currentTimeMillis()
        logger.info("begin auto renewal|$projectCode")
        do {
            val authResourceList = authResourceService.list(
                projectCode = projectCode,
                resourceType = null,
                resourceName = null,
                offset = offset,
                limit = limit
            )
            val resourceSize = authResourceList.size
            logger.info("auto renewal size|$projectCode|$offset|$resourceSize")
            authResourceList.forEach { authResource ->
                val resourceType = authResource.resourceType
                val resourceCode = authResource.resourceCode
                try {
                    permissionResourceMemberService.autoRenewal(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        validExpiredDay = validExpiredDay
                    )
                } catch (ignored: Throwable) {
                    logger.error("Failed to auto renewal|$projectCode|$resourceType|$resourceCode")
                }
            }
            offset += limit
        } while (resourceSize == limit)
        logger.info("Finish to auto renewal|$projectCode|${System.currentTimeMillis() - startTime}")
    }

    override fun migrateResourceAuthorization(projectCodes: List<String>): Boolean {
        return migrateResourceAuthorizationService.migrateResourceAuthorization(
            projectCodes = projectCodes
        )
    }

    override fun migrateAllResourceAuthorization(): Boolean {
        return migrateResourceAuthorizationService.migrateAllResourceAuthorization()
    }

    override fun fixResourceGroups(projectCodes: List<String>): Boolean {
        projectCodes.forEach {
            migrateResourceGroupService.fixResourceGroups(
                projectCode = it
            )
        }
        return true
    }

    override fun enablePipelineListPermissionControl(projectCodes: List<String>): Boolean {
        projectCodes.forEach {
            val projectInfo = client.get(ServiceProjectResource::class).get(it).data!!
            val properties = projectInfo.properties ?: ProjectProperties()
            properties.pipelineListPermissionControl = true
            logger.info("update project($it) properties|$properties")
            client.get(ServiceProjectResource::class).updateProjectProperties(it, properties)
        }
        return true
    }
}
