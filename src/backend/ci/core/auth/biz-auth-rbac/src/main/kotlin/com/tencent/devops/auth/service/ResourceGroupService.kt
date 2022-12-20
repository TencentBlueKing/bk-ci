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

import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthDefaultGroupDao
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class ResourceGroupService(
    private val dslContext: DSLContext,
    private val authDefaultGroupDao: AuthDefaultGroupDao,
    private val iamV2ManagerService: V2ManagerService,
) {

    fun createDefaultGroup(
        subsetManagerId: Int,
        userId: String,
        resourceType: String,
        resourceName: String
    ) {
        val defaultGroups = authDefaultGroupDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            createMode = false
        )
        val managerRoleGroups = defaultGroups.filter {
            it.groupCode != DefaultGroupType.MANAGER.name
        }.map { defaultGroup ->
            val name = IamGroupUtils.buildSubsetManagerGroupName(
                resourceName = resourceName,
                groupName = defaultGroup.groupName
            )
            val description = IamGroupUtils.buildSubsetManagerGroupDescription(
                resourceName = resourceName,
                groupName = name,
                userId = userId
            )
            ManagerRoleGroup(name, description, true)
        }
        val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(managerRoleGroups).build()
        iamV2ManagerService.batchCreateSubsetRoleGroup(subsetManagerId, managerRoleGroupDTO)
    }
}
