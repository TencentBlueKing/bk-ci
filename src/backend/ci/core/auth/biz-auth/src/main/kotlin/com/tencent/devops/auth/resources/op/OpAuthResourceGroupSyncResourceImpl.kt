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
 */

package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.sync.OpAuthResourceGroupSyncResource
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAuthResourceGroupSyncResourceImpl @Autowired constructor(
    private val permissionResourceGroupSyncService: PermissionResourceGroupSyncService
) : OpAuthResourceGroupSyncResource {

    override fun syncByCondition(projectConditionDTO: ProjectConditionDTO): Result<Boolean> {
        permissionResourceGroupSyncService.syncByCondition(projectConditionDTO)
        return Result(true)
    }

    override fun syncGroupMemberExpiredTime(projectConditionDTO: ProjectConditionDTO): Result<Boolean> {
        permissionResourceGroupSyncService.syncGroupMemberExpiredTime(projectConditionDTO)
        return Result(true)
    }

    override fun batchSyncGroupAndMember(projectIds: List<String>): Result<Boolean> {
        permissionResourceGroupSyncService.batchSyncGroupAndMember(projectIds)
        return Result(true)
    }

    override fun batchSyncProjectGroup(projectIds: List<String>): Result<Boolean> {
        permissionResourceGroupSyncService.batchSyncProjectGroup(projectIds)
        return Result(true)
    }

    override fun batchSyncAllMember(projectIds: List<String>): Result<Boolean> {
        permissionResourceGroupSyncService.batchSyncAllMember(projectIds)
        return Result(true)
    }

    override fun syncResourceMember(projectId: String, resourceType: String, resourceCode: String): Result<Boolean> {
        permissionResourceGroupSyncService.syncResourceMember(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return Result(true)
    }

    override fun fixResourceGroupMember(projectId: String): Result<Boolean> {
        permissionResourceGroupSyncService.fixResourceGroupMember(projectId)
        return Result(true)
    }

    override fun syncIamGroupMembersOfApply(): Result<Boolean> {
        permissionResourceGroupSyncService.syncIamGroupMembersOfApply()
        return Result(true)
    }
}
