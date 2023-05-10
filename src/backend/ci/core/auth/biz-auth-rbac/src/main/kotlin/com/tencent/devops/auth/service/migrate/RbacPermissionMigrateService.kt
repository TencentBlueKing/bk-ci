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

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.pojo.ProjectVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

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
        private const val V0_AUTH_TYPE = "v0"
        private const val V3_AUTH_TYPE = "v3"
    }

    @Value("\${auth.migrateProjectTag:#{null}}")
    private val migrateProjectTag: String = ""

    override fun v3ToRbacAuth(projectCodes: List<String>): Boolean {
        logger.info("migrate $projectCodes auth from v3 to rbac")
        val projectVos =
            client.get(ServiceProjectResource::class).listByProjectCode(projectCodes = projectCodes.toSet()).data
                ?: run {
                    logger.info("migrate project info is empty")
                    return false
                }
        // 1. 启动迁移任务
        migrateV3PolicyService.startMigrateTask(
            v3GradeManagerIds = projectVos.filter { !it.relationId.isNullOrBlank() }.map { it.relationId!! }
        )
        return projectCodes.map { projectCode ->
            migrateToRbacAuth(
                projectCode = projectCode,
                migrateTaskId = 0,
                authType = V3_AUTH_TYPE
            )
        }.all { it }
    }

    override fun v0ToRbacAuth(projectCodes: List<String>): Boolean {
        logger.info("migrate $projectCodes auth from v0 to rbac")
        // 1. 启动迁移任务
        val migrateTaskId = migrateV0PolicyService.startMigrateTask(
            projectCodes = projectCodes
        )
        return projectCodes.map { projectCode ->
            migrateToRbacAuth(
                projectCode = projectCode,
                migrateTaskId = migrateTaskId,
                authType = V0_AUTH_TYPE
            )
        }.all { it }
    }

    @Suppress("LongMethod", "ReturnCount", "ComplexMethod")
    private fun migrateToRbacAuth(
        projectCode: String,
        migrateTaskId: Int,
        authType: String
    ): Boolean {
        logger.info("Start migrate $projectCode from $authType to rbac")
        val startEpoch = System.currentTimeMillis()
        val watcher = Watcher("v3ToRbacAuth|$projectCode")
        try {
            val authMigrationInfo = authMigrationDao.get(
                dslContext = dslContext,
                projectCode = projectCode
            )
            if (authMigrationInfo != null && authMigrationInfo.status == AuthMigrateStatus.PENDING.value) {
                logger.info("project $projectCode is migrating")
                return false
            }
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data ?: run {
                logger.warn("project $projectCode not exist")
                return false
            }
            // 判断项目的创建人是否离职，若离职，则直接结束。并发出通知
            val isProjectCreatorNotExist = deptService.getUserInfo(
                userId = "greysonfang",
                name = projectInfo.creator!!
            ) == null
            if (isProjectCreatorNotExist) {
                logger.warn("project creator is not exist!|creator=${projectInfo.creator}")
                return false
            }

            authMigrationDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                status = AuthMigrateStatus.PENDING.value
            )

            // 创建分级管理员
            watcher.start("createGradeManager")
            val gradeManagerId = authResourceService.getOrNull(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )?.relationId?.toInt() ?: run {
                createGradeManager(projectCode, projectInfo)
            } ?: run {
                logger.info("project $projectCode gradle manager not found")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.CAN_NOT_FIND_RELATION
                )
            }
            // 迁移资源
            watcher.start("migrateResource")
            migrateResourceService.migrateResource(
                projectCode = projectCode,
                projectCreator = projectInfo.creator!!
            )

            when (authType) {
                V0_AUTH_TYPE -> {
                    migrateV0Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        migrateTaskId = migrateTaskId,
                        gradeManagerId = gradeManagerId,
                        watcher = watcher
                    )
                }
                V3_AUTH_TYPE -> {
                    migrateV3Auth(
                        projectCode = projectCode,
                        projectName = projectInfo.projectName,
                        gradeManagerId = gradeManagerId,
                        watcher = watcher
                    )
                }
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
        } catch (ignore: Exception) {
            logger.error("Failed to migrate $projectCode from $authType to rbac", ignore)
            authMigrationDao.updateStatus(
                dslContext = dslContext,
                projectCode = projectCode,
                status = AuthMigrateStatus.FAILED.value,
                totalTime = null
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
        val compareResult = migrateResultService.compare(projectCode = projectCode)
        if (!compareResult) {
            logger.warn("Failed to compare $projectCode policy")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
                params = arrayOf(projectCode)
            )
        }
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
        val compareResult = migrateResultService.compare(projectCode = projectCode)
        if (!compareResult) {
            logger.warn("Failed to compare $projectCode policy")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
                params = arrayOf(projectCode)
            )
        }
    }

    private fun createGradeManager(
        projectCode: String,
        projectInfo: ProjectVO
    ): Int? {
        client.get(ServiceProjectApprovalResource::class).createMigration(projectId = projectCode)
        permissionResourceService.resourceCreateRelation(
            userId = projectInfo.creator ?: "",
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectInfo.projectName
        )
        return authResourceService.getOrNull(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )?.relationId?.toInt()
    }
}
