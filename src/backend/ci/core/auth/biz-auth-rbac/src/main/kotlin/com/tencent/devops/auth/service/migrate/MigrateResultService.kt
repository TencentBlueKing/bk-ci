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
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.trace.TraceTag
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
    private val migrateResourceService: MigrateResourceService
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
                        if (compare(
                                projectCode = projectCode,
                                resourceType = resourceType
                            )) {
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

    private fun compare(
        projectCode: String,
        resourceType: String
    ): Boolean {
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE
        var hasMore = true
        while (hasMore) {
            val verifyRecordList = authVerifyRecordService.list(
                projectCode = projectCode,
                resourceType = resourceType,
                offset = offset,
                limit = limit
            )
            verifyRecordList.filter { it.verifyResult }.forEach {
                with(it) {
                    if (isSkipCompare(resourceCode = resourceCode, action = action)) return@forEach
                    val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        migrateResourceCode = resourceCode
                    )
                    val rbacVerifyResult = rbacResourceCode?.let {
                        permissionService.validateUserResourcePermissionByRelation(
                            userId = userId,
                            action = action,
                            projectCode = projectId,
                            resourceCode = rbacResourceCode,
                            resourceType = resourceType,
                            relationResourceType = null
                        )
                    } ?: false
                    if (!rbacVerifyResult) {
                        val checkResource = migrateResourceService.fetchInstanceInfo(
                            resourceType = resourceType,
                            projectCode = projectCode,
                            ids = listOf(resourceCode)
                        )?.data?.isEmpty() ?: true
                        if (checkResource) {
                            logger.info("compare policy,resource not found|$projectId|$resourceType|$resourceCode")
                            return@forEach
                        }
                        logger.error("Failed to compare policy:$userId|$action|$projectId|$resourceType|$resourceCode")
                        throw ErrorCodeException(
                            errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
                            params = arrayOf(projectCode),
                            defaultMessage = "Failed to compare policy:" +
                                "$userId|$action|$projectId|$resourceType|$resourceCode"
                        )
                    }
                }
            }
            hasMore = limit == verifyRecordList.size
            offset += limit
        }
        return true
    }

    private fun isSkipCompare(
        resourceCode: String,
        action: String
    ): Boolean {
        return resourceCode == "*" ||
            action.substringAfterLast("_") == AuthPermission.DELETE.value ||
            action == "all_action"
    }
}
