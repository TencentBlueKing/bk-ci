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
import com.tencent.bk.sdk.iam.dto.CreateRelationDTO
import com.tencent.bk.sdk.iam.dto.grant.AncestorsApiReq
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.IamCacheRefreshEvent
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxPermissionExtServiceImpl @Autowired constructor(
    val managerService: ManagerService,
    val iamConfiguration: IamConfiguration,
    val iamCacheService: IamCacheService,
    val authRefreshDispatch: AuthRefreshDispatch
) : PermissionExtService {
    override fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resourceCreateRelation $userId $projectCode $resourceCode $resourceName $resourceType")
        val ancestors = mutableListOf<AncestorsApiReq>()
        if (resourceType != AuthResourceType.PROJECT.value) {
            ancestors.add(AncestorsApiReq(iamConfiguration.systemId, AuthResourceType.PROJECT.value, projectCode))
        }
        val createRelationDTO = CreateRelationDTO(
            iamConfiguration.systemId,
            resourceType,
            resourceCode,
            resourceName,
            userId,
            ancestors
        )
        logger.info("resourceCreateRelation $createRelationDTO")
        try {
            managerService.createResourceRelation(createRelationDTO)
            // 刷新所有实例iam缓存
            authRefreshDispatch.dispatch(
                IamCacheRefreshEvent(
                    userId = userId,
                    refreshType = "IamCacheRefresh",
                    resourceType = resourceType
                )
            )
        } catch (e: Exception) {
            logger.warn("resourceCreateRelation fail, $userId $resourceCode $resourceType $e")
            return false
        }
        return true
    }

    override fun resourceModifyRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ) = true

    override fun resourceDeleteRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) = true

    override fun resourceCancelRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) = true

    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionExtServiceImpl::class.java)
    }
}
