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

import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.pojo.dto.MigrateProjectDTO
import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.pojo.ProjectVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * rbac迁移服务
 */
@Suppress("LongParameterList")
class RbacPermissionMigrateService constructor(
    private val client: Client,
    private val migrateResourceService: MigrateResourceService,
    private val migrateV3PolicyService: MigrateV3PolicyService,
    private val migrateV0PolicyService: MigrateV0PolicyService,
    private val migrateResultService: MigrateResultService,
    private val permissionResourceService: PermissionResourceService,
    private val authResourceService: AuthResourceService,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao,
    private val deptService: DeptService
) : PermissionMigrateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionMigrateService::class.java)
        private const val ALL_MEMBERS = "*"
        private const val ALL_MEMBERS_NAME = "allMembersName"
        private val allToRbacExecutorService = Executors.newFixedThreadPool(2)
        private val migrateProjectsExecutorService = Executors.newFixedThreadPool(10)
    }

    @Value("\${auth.migrateProjectTag:#{null}}")
    private val migrateProjectTag: String = ""

    override fun v3ToRbacAuth(migrateProjects: List<MigrateProjectDTO>): Boolean {
        logger.info("migrate $migrateProjects auth from v3 to rbac")
        val projectVos =
            client.get(ServiceProjectResource::class).listByProjectCode(
                projectCodes = migrateProjects.map { it.projectCode }.toSet()
            ).data ?: run {
                logger.info("migrate project info is empty")
                return false
            }
        return v3ToRbacAuth(
            migrateProjects = migrateProjects,
            projectVos = projectVos
        )
    }

    private fun v3ToRbacAuth(
        migrateProjects: List<MigrateProjectDTO>,
        projectVos: List<ProjectVO>
    ): Boolean {
        val migrateProjectRelationIds = projectVos.filter { !it.relationId.isNullOrBlank() }.map { it.relationId!! }
        // 1. 启动迁移任务
        migrateV3PolicyService.startMigrateTask(
            v3GradeManagerIds = migrateProjectRelationIds
        )
        migrateProjects.forEach { migrateProject ->
            CompletableFuture.supplyAsync(
                {
                    migrateToRbacAuth(
                        migrateProject = migrateProject,
                        migrateTaskId = 0,
                        authType = AuthSystemType.V3_AUTH_TYPE
                    )
                },
                migrateProjectsExecutorService
            )
        }
        return true
    }

    override fun v0ToRbacAuth(migrateProjects: List<MigrateProjectDTO>): Boolean {
        logger.info("migrate $migrateProjects auth from v0 to rbac")
        // 1. 启动迁移任务
        val migrateTaskId = migrateV0PolicyService.startMigrateTask(
            projectCodes = migrateProjects.map { it.projectCode }
        )
        migrateProjects.forEach { migrateProject ->
            CompletableFuture.supplyAsync(
                {
                    migrateToRbacAuth(
                        migrateProject = migrateProject,
                        migrateTaskId = migrateTaskId,
                        authType = AuthSystemType.V0_AUTH_TYPE
                    )
                },
                migrateProjectsExecutorService
            )
        }
        return true
    }

    override fun allToRbacAuth(): Boolean {
        allToRbacExecutorService.submit {
            var offset = 0
            val limit = 50
            do {
                val migrateProjects = client.get(ServiceProjectResource::class).listMigrateProjects(
                    limit = limit,
                    offset = offset
                ).data ?: break
                val v3MigrateProjects =
                    migrateProjects.filter {
                        it.routerTag == null ||
                            it.routerTag == AuthSystemType.V3_AUTH_TYPE.value
                    }.map { MigrateProjectDTO(approver = null, projectCode = it.englishName) }
                logger.info("migrate all project to rbac|v3MigrateProjects:$v3MigrateProjects")
                val v0MigrateProjects =
                    migrateProjects.filter { it.routerTag == AuthSystemType.V0_AUTH_TYPE.value }
                        .map { MigrateProjectDTO(approver = null, projectCode = it.englishName) }
                logger.info("migrate all project to rbac|v0MigrateProjects:$v0MigrateProjects")
                if (v3MigrateProjects.isNotEmpty()) {
                    v3ToRbacAuth(migrateProjects = v3MigrateProjects)
                }
                if (v0MigrateProjects.isNotEmpty()) {
                    v0ToRbacAuth(migrateProjects = v0MigrateProjects)
                }
                offset += limit
            } while (migrateProjects.size == limit)
        }
        return true
    }

    @Suppress("LongMethod", "ReturnCount", "ComplexMethod")
    private fun migrateToRbacAuth(
        migrateProject: MigrateProjectDTO,
        migrateTaskId: Int,
        authType: AuthSystemType
    ): Boolean {
        val projectCode = migrateProject.projectCode
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
            // 判断项目的创建人是否离职，若离职并且未指定新创建人，则直接结束。
            val iamApprover = buildResourceCreator(
                approver = migrateProject.approver,
                projectCreator = projectInfo.creator!!
            )
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
                    iamApprover = iamApprover
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
                iamApprover = iamApprover
            )

            when (authType) {
                AuthSystemType.V0_AUTH_TYPE -> {
                    migrateV0Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        migrateTaskId = migrateTaskId,
                        gradeManagerId = gradeManagerId,
                        watcher = watcher
                    )
                }
                AuthSystemType.V3_AUTH_TYPE -> {
                    migrateV3Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        gradeManagerId = gradeManagerId,
                        watcher = watcher
                    )
                }
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
        } catch (exception: Exception) {
            handleException(
                exception = exception,
                projectCode = projectCode,
                authType = authType.value
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
            gradeManagerId = gradeManagerId
        )
        // 迁移用户自定义权限
        watcher.start("migrateUserCustomPolicy")
        migrateV3PolicyService.migrateUserCustomPolicy(
            projectCode = projectCode
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
        watcher: Watcher
    ) {
        // 轮询任务状态
        migrateV0PolicyService.loopTaskStatus(migrateTaskId = migrateTaskId)
        // 迁移v0用户组
        watcher.start("migrateGroupPolicy")
        migrateV0PolicyService.migrateGroupPolicy(
            projectCode = projectCode,
            projectName = projectName,
            gradeManagerId = gradeManagerId
        )
        // 迁移用户自定义权限
        watcher.start("migrateUserCustomPolicy")
        migrateV0PolicyService.migrateUserCustomPolicy(
            projectCode = projectCode
        )
        // 对比迁移结果
        watcher.start("comparePolicy")
        migrateResultService.compare(projectCode = projectCode)
    }

    private fun createGradeManager(
        projectCode: String,
        projectInfo: ProjectVO,
        iamApprover: String
    ): Int? {
        client.get(ServiceProjectApprovalResource::class).createMigration(projectId = projectCode)
        permissionResourceService.resourceCreateRelation(
            userId = iamApprover,
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

    private fun buildResourceCreator(
        approver: String?,
        projectCreator: String
    ): String {
        val isProjectCreatorLeaveOffice = deptService.getUserInfo(
            userId = "admin",
            name = projectCreator
        ) == null
        if (isProjectCreatorLeaveOffice && approver == null) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_CREATOR_NOT_EXIST,
                defaultMessage = "project creator not exist $projectCreator"
            )
        }
        return approver ?: projectCreator
    }

    private fun handleException(
        exception: Exception,
        projectCode: String,
        authType: String
    ) {
        val errorMessage = when (exception) {
            is IamException -> {
                exception.errorMsg
            }
            is ErrorCodeException -> {
                exception.defaultMessage
            }
            else -> {
                exception.toString()
            }
        }
        logger.error("Failed to migrate $projectCode from $authType to rbac", exception)
        authMigrationDao.updateStatus(
            dslContext = dslContext,
            projectCode = projectCode,
            status = AuthMigrateStatus.FAILED.value,
            errorMessage = errorMessage,
            totalTime = null
        )
    }
}
