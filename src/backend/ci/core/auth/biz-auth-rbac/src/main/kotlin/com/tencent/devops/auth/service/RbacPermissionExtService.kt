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

import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import org.slf4j.LoggerFactory

class RbacPermissionExtService(
    private val permissionResourceService: PermissionResourceService
) : PermissionExtService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionExtService::class.java)
    }

    override fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resourceCreateRelation $userId|$projectCode|$resourceCode|$resourceName|$resourceType")
        return permissionResourceService.resourceCreateRelation(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun resourceModifyRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resourceModifyRelation $projectCode|$resourceCode|$resourceName|$resourceType")
        return permissionResourceService.resourceModifyRelation(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun resourceDeleteRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("resourceDeleteRelation $projectCode|$resourceCode|$resourceType")
        return permissionResourceService.resourceDeleteRelation(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun resourceCancelRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("resourceCancelRelation $projectCode|$resourceCode|$resourceType")
        return permissionResourceService.resourceCancelRelation(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }
}
