/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkiam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bkrepo.auth.pojo.AncestorsApiReq
import com.tencent.bkrepo.auth.pojo.IamCreateApiReq
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.SystemCode
import com.tencent.bkrepo.auth.util.BkiamUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BkiamServiceImpl @Autowired constructor(
    private val iamConfiguration: IamConfiguration,
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamEsbClient: IamEsbClient
) : BkiamService {
    override fun validateResourcePermission(
        userId: String,
        systemCode: SystemCode,
        projectId: String,
        resourceType: ResourceType,
        action: PermissionAction,
        resourceId: String
    ): Boolean {
        logger.info("validateResourcePermission, userId: $userId, projectId: $projectId, systemCode: $systemCode," +
            " resourceType: $resourceType, action: $action, resourceId: $resourceId")
        val resourceAction = BkiamUtils.buildAction(resourceType, action)
        if (systemCode == SystemCode.BK_REPO && resourceType == ResourceType.PROJECT) {
            return authHelper.isAllowed(userId, resourceAction)
        }

        val instanceDTO = InstanceDTO()
        instanceDTO.system = systemCode.id()
        instanceDTO.id = resourceId
        instanceDTO.type = resourceType.id()

        val path = PathInfoDTO()
        path.type = ResourceType.PROJECT.id()
        path.id = projectId
        instanceDTO.path = path

        return authHelper.isAllowed(userId, resourceAction, instanceDTO)
    }

    override fun listResourceByPermission(
        userId: String,
        systemCode: SystemCode,
        projectId: String,
        resourceType: ResourceType,
        action: PermissionAction
    ): List<String> {
        logger.info("listResourceByPermission, userId: $userId, projectId: $projectId, systemCode: $systemCode," +
            " resourceType: $resourceType, action: $action")
        val actionDto = ActionDTO()
        actionDto.id = BkiamUtils.buildAction(resourceType, action)
        val expression = (policyService.getPolicyByAction(userId, actionDto, null) ?: return emptyList())
        logger.debug("listResourceByPermission, expression: $expression")
        if (expression.operator == null && expression.content == null) {
            return emptyList()
        }
        if (expression.operator == ExpressionOperationEnum.ANY) {
            return listOf("*")
        }

        return if (resourceType == ResourceType.PROJECT) {
            BkiamUtils.getProjects(expression)
        } else {
            val instancesList = BkiamUtils.getResourceInstance(expression, projectId, resourceType)
            logger.debug("getUserResourceByPermission getInstance project[$projectId], type[${resourceType.id()}]," +
                " instances[$instancesList]")
            if (!instancesList.contains("*")) {
                instancesList.toList()
            } else {
                listOf("*")
            }
        }
    }

    override fun createResource(
        userId: String,
        systemCode: SystemCode,
        projectId: String,
        resourceType: ResourceType,
        resourceId: String,
        resourceName: String
    ) {
        logger.info("createResource, userId: $userId, projectId: $projectId, systemCode: $systemCode," +
            " resourceType: $resourceType, resourceId: $resourceId, resourceName: $resourceName")
        val ancestors = mutableListOf<AncestorsApiReq>()
        if (resourceType != ResourceType.PROJECT) {
            ancestors.add(
                AncestorsApiReq(
                    system = iamConfiguration.systemId,
                    id = projectId,
                    type = ResourceType.PROJECT.id()
                )
            )
        }
        val iamApiReq = IamCreateApiReq(
            creator = userId,
            name = resourceName,
            id = resourceId,
            type = resourceType.id(),
            system = iamConfiguration.systemId,
            ancestors = ancestors,
            bk_app_code = "",
            bk_app_secret = "",
            bk_username = userId
        )
        iamEsbClient.createRelationResource(iamApiReq)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkiamServiceImpl::class.java)
    }
}
