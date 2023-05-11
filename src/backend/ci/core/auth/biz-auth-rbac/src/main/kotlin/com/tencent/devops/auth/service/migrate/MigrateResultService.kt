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
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import org.slf4j.LoggerFactory

class MigrateResultService constructor(
    private val permissionService: PermissionService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val authVerifyRecordService: AuthVerifyRecordService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResultService::class.java)
    }

    @Suppress("NestedBlockDepth")
    fun compare(projectCode: String): Boolean {
        logger.info("start to compare policy|$projectCode")
        val startEpoch = System.currentTimeMillis()
        try {
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            do {
                val verifyRecordList = authVerifyRecordService.listByProjectCode(
                    projectCode = projectCode,
                    offset = offset,
                    limit = limit
                )
                verifyRecordList.forEach {
                    with(it) {
                        if (resourceCode == "*") return@forEach
                        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
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
                        if (verifyResult != rbacVerifyResult) {
                            logger.warn("compare policy failed:$userId|$action|$projectId|$resourceType|$resourceCode")
                            throw ErrorCodeException(
                                errorCode = AuthMessageCode.ERROR_MIGRATE_AUTH_COMPARE_FAIL,
                                params = arrayOf(projectCode),
                                defaultMessage = "compare policy failed:" +
                                    "$userId|$action|$projectId|$resourceType|$resourceCode"
                            )
                        }
                    }
                }
                offset += limit
            } while (verifyRecordList.size == limit)
            return true
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to compare policy|$projectCode"
            )
        }
    }
}
