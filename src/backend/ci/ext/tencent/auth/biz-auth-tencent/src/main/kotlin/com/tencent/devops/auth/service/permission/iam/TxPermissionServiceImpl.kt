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
 */

package com.tencent.devops.auth.service.permission.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import com.tencent.devops.auth.service.AuthPipelineIdService
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors

class TxPermissionServiceImpl @Autowired constructor(
    val authHelper: AuthHelper,
    val policyService: PolicyService,
    val iamConfiguration: IamConfiguration,
    val managerService: ManagerService,
    val iamCacheService: IamCacheService,
    val client: Client,
    val authPipelineIdService: AuthPipelineIdService,
    val authVerifyRecordService: AuthVerifyRecordService
) : AbsPermissionService(authHelper, policyService, iamConfiguration, iamCacheService) {

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return super.validateUserActionPermission(userId, action)
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        if (reviewManagerCheck(
                userId = userId,
                projectCode = projectCode,
                resourceTypeStr = resourceType ?: "",
                action = action
            )) {
            return true
        }
        return super.validateUserResourcePermission(userId, action, projectCode, resourceType)
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        if (reviewManagerCheck(
                userId = userId,
                projectCode = projectCode,
                resourceTypeStr = resourceType ?: "",
                action = action
            )) {
            return true
        }

        // 如果校验的资源为pipeline,需要兼容repo传pipelineId的情况
        val useResourceCode = authPipelineIdService.findPipelineAutoId(resourceType, resourceCode)

        // action需要兼容repo只传AuthPermission的情况,需要组装为V3的action
        val useAction = if (!action.contains("_")) {
            TActionUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resourceType))
        } else {
            action
        }
        val verifyResult = super.validateUserResourcePermissionByRelation(
            userId = userId,
            action = useAction,
            projectCode = projectCode,
            resourceCode = useResourceCode,
            resourceType = resourceType,
            relationResourceType = relationResourceType
        )
        logger.info("The system starts recording the verify result:$verifyResult|$resourceCode|$useAction")
        executor.submit {
            authVerifyRecordService.createOrUpdateVerifyRecord(
                VerifyRecordDTO(
                    userId = userId,
                    projectId = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    action = useAction,
                    verifyResult = verifyResult
                )
            )
        }
        return verifyResult
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        if (reviewManagerCheck(
                userId = userId,
                projectCode = projectCode,
                resourceTypeStr = resourceType ?: "",
                action = action
            )) {
            return arrayListOf("*")
        }
        val permissionResult = super.getUserResourceByAction(userId, action, projectCode, resourceType)
        if (permissionResult.isNotEmpty()) {
            executor.submit {
                authVerifyRecordService.bathCreateOrUpdateVerifyRecord(
                    permissionsResourcesMap = mapOf(AuthPermission.get(action.substringAfter("_")) to permissionResult),
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            }
        }
        return permissionResult
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val permissionResourcesMap = super.getUserResourcesByActions(userId, actions, projectCode, resourceType)
        executor.submit {
            authVerifyRecordService.bathCreateOrUpdateVerifyRecord(
                permissionsResourcesMap = permissionResourcesMap,
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType
            )
        }
        return permissionResourcesMap
    }

    private fun reviewManagerCheck(
        userId: String,
        projectCode: String,
        resourceTypeStr: String,
        action: String
    ): Boolean {
        if (action == "all_action" || resourceTypeStr.isNullOrEmpty()) {
            return false
        }
        return managerService.isManagerPermission(
            userId = userId,
            projectId = projectCode,
            resourceType = TActionUtils.getResourceTypeByStr(resourceTypeStr),
            authPermission = TActionUtils.getAuthPermissionByAction(action)
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionServiceImpl::class.java)
        private val executor = Executors.newFixedThreadPool(5)
    }
}
