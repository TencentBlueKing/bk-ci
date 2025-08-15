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

package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.dto.ListGroupConditionDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup

class SamplePermissionResourceGroupService : PermissionResourceGroupService {

    override fun listGroup(
        userId: String,
        listGroupConditionDTO: ListGroupConditionDTO
    ): Pagination<IamGroupInfoVo> {
        return Pagination(false, emptyList())
    }

    override fun listUserBelongGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): List<IamGroupMemberInfoVo> {
        return emptyList()
    }

    override fun listIamGroupIdsByGroupName(
        projectId: String,
        groupName: String
    ): List<Int> = emptyList()

    override fun deleteGroup(
        userId: String?,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        return true
    }

    override fun createGroup(
        projectId: String,
        groupAddDTO: GroupAddDTO
    ): Int = 0

    override fun renameGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        renameGroupDTO: RenameGroupDTO
    ): Boolean {
        return true
    }

    override fun createGroupAndPermissionsByGroupCode(
        projectId: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        groupName: String?,
        groupDesc: String?
    ): Int = 0

    override fun createCustomGroupAndPermissions(
        projectId: String,
        customGroupCreateReq: CustomGroupCreateReq
    ): Int = 0

    override fun getByGroupCode(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: BkAuthGroup
    ): AuthResourceGroup? = null

    override fun listProjectMemberGroupTemplateIds(
        projectCode: String,
        memberId: String
    ): List<String> {
        return emptyList()
    }

    override fun syncManagerGroup(
        projectCode: String,
        managerId: Int,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String
    ): Boolean = true

    override fun deleteManagerDefaultGroup(
        userId: String,
        managerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean = true

    override fun modifyManagerDefaultGroup(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean = true
}
