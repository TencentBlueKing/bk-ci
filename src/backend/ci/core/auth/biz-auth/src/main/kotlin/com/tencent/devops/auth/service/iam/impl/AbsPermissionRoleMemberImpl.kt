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

package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleMemberDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerGroupMemberVo
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

abstract class AbsPermissionRoleMemberImpl @Autowired constructor(
    open val iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService
) : PermissionRoleMemberService {

    override fun createRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<String>,
        managerGroup: Boolean
    ) {
        permissionGradeService.checkGradeManagerUser(projectId = projectId, userId = userId)
        val roleMembers = mutableListOf<ManagerMember>()
        members.forEach {
            checkUser(it)
            roleMembers.add(ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), it))
        }
        val expiredTime = System.currentTimeMillis()/1000 + TimeUnit.DAYS.toSeconds(expiredAt)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder().expiredAt(expiredTime).members(roleMembers).build()
        iamManagerService.createRoleGroupMember(roleId, managerMemberGroupDTO)

        // 添加用户到管理员需要同步添加用户到分级管理员
        if (managerGroup) {
            val gradeMembers = ManagerRoleMemberDTO.builder().members(members).build()
            iamManagerService.batchCreateGradeManagerRoleMember(gradeMembers, projectId)
        }
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<String>,
        managerGroup: Boolean
    ) {
    }

    override fun getRoleMember(projectId: Int, roleId: Int): ManagerGroupMemberVo {
        val pageInfoDTO = PageInfoDTO()
        pageInfoDTO.limit = 1000
        pageInfoDTO.offset = 0
        return iamManagerService.getRoleGroupMember(roleId, pageInfoDTO)
    }

    abstract fun checkUser(userId: String)

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionRoleMemberImpl::class.java)
        const val expiredAt = 365L
    }
}
