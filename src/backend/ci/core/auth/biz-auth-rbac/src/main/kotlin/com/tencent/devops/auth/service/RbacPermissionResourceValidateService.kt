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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.LogUtils
import org.slf4j.LoggerFactory

class RbacPermissionResourceValidateService(
    private val permissionService: PermissionService,
    private val rbacCacheService: RbacCacheService
) : PermissionResourceValidateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceValidateService::class.java)
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Map<String, Boolean> {
        logger.info("batch validate user resource permission|$userId|$projectCode|$permissionBatchValidateDTO")
        val watcher = Watcher("batchValidateUserResourcePermission|$userId|$projectCode")
        try {
            val projectActionList = mutableSetOf<String>()
            val resourceActionList = mutableSetOf<String>()

            permissionBatchValidateDTO.actionList.forEach { action ->
                val actionInfo = rbacCacheService.getActionInfo(action)
                val iamRelatedResourceType = actionInfo.relatedResourceType
                if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
                    projectActionList.add(action)
                } else {
                    resourceActionList.add(action)
                }
            }

            val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
            // 验证项目下的权限
            if (projectActionList.isNotEmpty()) {
                watcher.start("batchValidateProjectAction")
                actionCheckPermissionMap.putAll(
                    validateProjectPermission(
                        userId = userId,
                        actions = projectActionList.toList(),
                        projectCode = projectCode
                    )
                )
            }
            // 验证具体资源权限
            if (resourceActionList.isNotEmpty()) {
                watcher.start("batchValidateResourceAction")
                actionCheckPermissionMap.putAll(
                    validateResourcePermission(
                        userId = userId,
                        projectCode = projectCode,
                        actions = resourceActionList.toList(),
                        resourceType = permissionBatchValidateDTO.resourceType,
                        resourceCode = permissionBatchValidateDTO.resourceCode
                    )
                )
            }
            return actionCheckPermissionMap
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    private fun validateProjectPermission(
        userId: String,
        actions: List<String>,
        projectCode: String
    ): Map<String, Boolean> {
        return permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value
        )
    }

    private fun validateResourcePermission(
        userId: String,
        projectCode: String,
        actions: List<String>,
        resourceType: String,
        resourceCode: String
    ): Map<String, Boolean> {
        return permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }
}
