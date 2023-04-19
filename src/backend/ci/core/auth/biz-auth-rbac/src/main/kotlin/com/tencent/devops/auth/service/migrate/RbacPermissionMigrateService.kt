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
    private val permissionResourceService: PermissionResourceService,
    private val authResourceService: AuthResourceService,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao
) : PermissionMigrateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionMigrateService::class.java)
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
        projectCodes.forEach { projectCode ->
            v3ToRbacAuth(projectCode)
        }
        return true
    }

    @Suppress("LongMethod", "ReturnCount")
    fun v3ToRbacAuth(projectCode: String): Boolean {
        logger.info("Start migrate $projectCode from v3 to rbac")
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
                projectCode = projectCode
            )
            // 迁移v3用户组
            watcher.start("migrateGroupPolicy")
            migrateV3PolicyService.migrateGroupPolicy(
                projectCode = projectCode,
                projectName = projectInfo.projectName,
                gradeManagerId = gradeManagerId
            )
            // 迁移用户自定义权限
            watcher.start("migrateUserCustomPolicy")
            migrateV3PolicyService.migrateUserCustomPolicy(
                projectCode = projectCode
            )
            // 对比迁移结果
            watcher.start("comparePolicy")
            val compareResult = migrateV3PolicyService.comparePolicy(projectCode = projectCode)
            if (!compareResult) {
                logger.warn("Failed to compare $projectCode policy")
                return false
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
            logger.error("Failed to migrate $projectCode from v3 to rbac", ignore)
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
