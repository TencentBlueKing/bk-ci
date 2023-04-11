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

import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.pojo.enum.AuthMigrateStatus
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.PermissionGradeManagerService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
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
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val authResourceService: AuthResourceService,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao
): PermissionMigrateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionMigrateService::class.java)
        private val executorService = Executors.newFixedThreadPool(2)
    }

    @Value("\${migrate.projectTag:#{\"\"}}")
    private val migrateProjectTag: String = ""

    override fun v3ToRbacAuth(projectCode: String): Boolean {
        logger.info("Start migrate $projectCode from v3 to rbac")
        val startEpoch = System.currentTimeMillis()
        try {
            val authMigrationInfo = authMigrationDao.get(
                dslContext = dslContext,
                projectCode = projectCode
            )
            if (authMigrationInfo != null && authMigrationInfo.status == AuthMigrateStatus.PENDING.value) {
                logger.info("project $projectCode is migrating")
                return false
            }
            authMigrationDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                status = AuthMigrateStatus.PENDING.value
            )
            // 1. 启动迁移任务
            migrateV3PolicyService.startMigrateTask()
            // 2. 创建分级管理员
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data ?: run {
                logger.warn("project $projectCode not exist")
                return false
            }
            if (projectInfo.routerTag == migrateProjectTag) {
                logger.info("project $projectCode has been migrated")
                return true
            }
            val gradeManagerId = authResourceService.getOrNull(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )?.relationId?.toInt() ?: run {
                permissionGradeManagerService.migrateGradeManager(
                    projectCode = projectCode,
                    projectName = projectInfo.projectName
                )
            }
            // 3. 异步迁移资源
            val resourceFuture = CompletableFuture.supplyAsync(
                { migrateResourceService.migrateResource(projectCode = projectCode) },
                executorService
            )
            // 4. 异步迁移v3用户组
            val policyFuture = CompletableFuture.supplyAsync(
                { migrateV3PolicyService.migrateGroupPolicy(projectCode = projectCode, gradeManagerId = gradeManagerId) },
                executorService
            )
            // 5. 等待资源和用户组迁移完成
            CompletableFuture.allOf(resourceFuture, policyFuture).join()
            // 7. 迁移用户自定义权限
            migrateV3PolicyService.migrateUserCustomPolicy(projectCode = projectCode, gradeManagerId = gradeManagerId)
            // 8. 对比迁移结果
            migrateV3PolicyService.comparePolicy(projectCode = projectCode)
            // 9. 设置项目路由tag
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
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to migrate $projectCode")
        }
    }
}
