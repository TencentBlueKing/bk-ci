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

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("ALL")
class MigrateResultService constructor(
    private val permissionService: PermissionService,
    private val rbacCacheService: RbacCacheService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val authVerifyRecordService: AuthVerifyRecordService,
    private val migrateResourceService: MigrateResourceService,
    private val authResourceService: AuthResourceService,
    private val deptService: DeptService,
    private val client: Client,
    private val tokenService: ClientTokenService,
    private val bkTag: BkTag,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResultService::class.java)
        private val executorService = Executors.newFixedThreadPool(50)
    }

    fun compare(projectCode: String): Boolean {
        logger.info("start to compare policy|$projectCode")
        val startEpoch = System.currentTimeMillis()
        try {
            val resourceTypes = rbacCacheService.listResourceTypes()
                .map { it.resourceType }
            val traceId = MDC.get(TraceTag.BIZID)
            val compareFuture = resourceTypes.map { resourceType ->
                CompletableFuture.supplyAsync(
                    {
                        MDC.put(TraceTag.BIZID, traceId)
                        if (compare(projectCode = projectCode, resourceType = resourceType)) {
                            logger.info("resourceType in project is successfully compared|$projectCode|$resourceType")
                        }
                    },
                    executorService
                )
            }
            CompletableFuture.allOf(*compareFuture.toTypedArray()).join()
            return true
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to compare policy|$projectCode"
            )
        }
    }

    private fun compare(projectCode: String, resourceType: String): Boolean {
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE
        do {
            val verifyRecordList = authVerifyRecordService.groupByResourceAndUserId(
                projectCode = projectCode,
                resourceType = resourceType,
                offset = offset,
                limit = limit
            )
            verifyRecordList.forEach {
                compareResource(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = it.resourceCode,
                    userId = it.userId
                )
            }
            offset += limit
        } while (limit == verifyRecordList.size)
        return true
    }

    private fun compareResource(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        userId: String
    ) {
        if (resourceCode == "*") {
            return
        }
        val actions = authVerifyRecordService.listResourceActions(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            userId = userId
        ).filter { !isSkipAction(it) }
        if (actions.isEmpty()) {
            return
        }
        // v0或v3资源转换成rbac不存在,说明资源可能已经被删除或者不是这个项目的
        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
            projectCode = projectCode,
            resourceType = resourceType,
            migrateResourceCode = resourceCode
        ) ?: return
        val rbacVerifyResultMap = permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceCode = rbacResourceCode,
            resourceType = resourceType
        )
        val exceptionActions = rbacVerifyResultMap.filter { !it.value }.keys.toList()
        // 如果迁移后的权限不匹配,则需要再次确认资源、用户和权限
        if (exceptionActions.isNotEmpty()) {
            reconfirm(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                rbacResourceCode = rbacResourceCode,
                actions = exceptionActions,
                userId = userId
            )
        }
    }

    private fun isSkipAction(action: String): Boolean {
        return action.substringAfterLast("_") == AuthPermission.DELETE.value || action == "all_action"
    }

    /**
     * 对比时可能存在以下情况导致校验失败
     * - 资源删除，但是校验表中记录没有删除
     * - 用户离职
     * - 用户在旧版权限已失效
     * 需要再次校验
     */
    private fun reconfirm(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        rbacResourceCode: String,
        actions: List<String>,
        userId: String
    ) {
        // 校验资源是否存在
        val resourceExists = checkResourceExists(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            rbacResourceCode = rbacResourceCode
        )
        if (!resourceExists) {
            logger.info(
                "resource does not exist or has been deleted, skip comparison|$projectCode|$resourceCode|$userId"
            )
            return
        }
        // 资源存在,校验资源是否迁移到rbac资源
        authResourceService.getOrNull(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = rbacResourceCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
            params = arrayOf(projectCode),
            defaultMessage = "Failed to compare policy:resource not migrate" +
                    "$userId|$projectCode|$resourceType|$resourceCode"
        )

        // 校验用户是否离职
        val userExists = deptService.getUserInfo(userId = "admin", name = userId) != null
        if (!userExists) {
            logger.info(
                "user does not exist or has left the company, skip comparison|$projectCode|$resourceCode|$userId"
            )
            return
        }
        // 用户存在,校验用户是否有资源权限
        val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectCode)
        val hasPermission = bkTag.invokeByTag(projectConsulTag) {
            // 此处需要注意,必须使用getGateway,不能使用get方法,因为ServicePermissionAuthResource的bean类是存在的,不会跨集群调用
            client.getGateway(ServicePermissionAuthResource::class).batchValidateUserResourcePermissionByRelation(
                userId = userId,
                token = tokenService.getSystemToken(null)!!,
                action = actions.map { it.substringAfterLast("_") },
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType
            )
        }.data!!
        logger.info("check user permission from $projectConsulTag|$projectCode|$resourceCode|$userId|$hasPermission")
        if (hasPermission) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
                params = arrayOf(projectCode),
                defaultMessage = "Failed to compare policy:permission not migrate" +
                        "$userId|$projectCode|$resourceType|$resourceCode|$actions"
            )
        }
    }

    private fun checkResourceExists(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        rbacResourceCode: String
    ): Boolean {
        // 校验资源是否存在
        return when (resourceType) {
            // 项目在迁移时已经校验是否存在
            AuthResourceType.PROJECT.value -> true
            // 记录表中记录的流水线ID和项目ID的关联关系可能有错误,回调接口查询资源信息没有传项目ID,导致流水线信息错误
            AuthResourceType.PIPELINE_DEFAULT.value -> {
                client.get(ServicePipelineResource::class)
                    .getPipelineId(projectCode = projectCode, pipelineId = rbacResourceCode).data != null
            }

            else -> {
                migrateResourceService.fetchInstanceInfo(
                    resourceType = resourceType,
                    projectCode = projectCode,
                    ids = listOf(resourceCode)
                )?.data?.isNotEmpty() ?: false
            }
        }
    }
}
