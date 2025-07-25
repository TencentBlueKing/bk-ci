/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 将资源组迁移到权限中心
 */
@Suppress("LongParameterList", "MagicNumber")
class MigrateResourceGroupService @Autowired constructor(
    private val authResourceService: AuthResourceService,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val iamV2ManagerService: V2ManagerService
) {
    fun fixResourceGroups(projectCode: String) {
        logger.info("start to fix resource groups,$projectCode ")
        val recordsOfNeedToFix = authResourceGroupDao.listRecordsOfNeedToFix(
            dslContext = dslContext,
            projectCode = projectCode
        )
        logger.info("resource groups need to fix ,$projectCode|$recordsOfNeedToFix")
        recordsOfNeedToFix.forEach { resourceGroupInfo ->
            val resourceInfo = authResourceService.get(
                projectCode = projectCode,
                resourceType = resourceGroupInfo.resourceType,
                resourceCode = resourceGroupInfo.resourceCode
            )
            val pageInfoDTO = V2PageInfoDTO()
            pageInfoDTO.page = PageUtil.DEFAULT_PAGE
            pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
            val iamGroupInfo = if (resourceInfo.resourceType == AuthResourceType.PROJECT.value) {
                val searchGroupDTO = SearchGroupDTO.builder()
                    .inherit(false)
                    .name(resourceGroupInfo.groupName)
                    .build()
                iamV2ManagerService.getGradeManagerRoleGroupV2(
                    resourceInfo.relationId,
                    searchGroupDTO,
                    pageInfoDTO
                )
            } else {
                iamV2ManagerService.getSubsetManagerRoleGroup(
                    resourceInfo.relationId.toInt(),
                    pageInfoDTO
                )
            }.results.firstOrNull { it.name == resourceGroupInfo.groupName }
            logger.info("resource groups need to fix,iam group info $projectCode|$iamGroupInfo")
            if (iamGroupInfo != null) {
                authResourceGroupDao.update(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceGroupInfo.resourceType,
                    resourceCode = resourceGroupInfo.resourceCode,
                    resourceName = resourceInfo.resourceName,
                    groupCode = resourceGroupInfo.groupCode,
                    groupName = resourceGroupInfo.groupName,
                    relationId = iamGroupInfo.id.toString()
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResourceGroupService::class.java)
    }
}
