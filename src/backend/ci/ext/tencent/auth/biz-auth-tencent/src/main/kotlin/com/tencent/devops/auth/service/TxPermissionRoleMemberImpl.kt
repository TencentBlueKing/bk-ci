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

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionRoleMemberImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired

@Service
class TxPermissionRoleMemberImpl @Autowired constructor(
    override val iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService,
    val groupService: AuthGroupService
) : AbsPermissionRoleMemberImpl(iamManagerService, permissionGradeService, groupService) {
    override fun createRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<RoleMemberDTO>,
        managerGroup: Boolean,
        checkAGradeManager: Boolean?
    ) {
        super.createRoleMember(userId, projectId, roleId, members, managerGroup, checkAGradeManager)
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        id: String,
        type: ManagerScopesEnum,
        managerGroup: Boolean
    ) {
        super.deleteRoleMember(userId, projectId, roleId, id, type, managerGroup)
    }
//
//    override fun getUserGroups(projectId: Int, userId: String): List<ManagerRoleGroupInfo>? {
//        val watcher = Watcher("getUserGroup$projectId$userId")
//        watcher.start("callIamUserGroup")
//        try {
//            val userSimpleGroup = super.getUserGroups(projectId, userId)
//            if (userSimpleGroup != null && userSimpleGroup.isNotEmpty()) {
//                return userSimpleGroup
//            }
//            logger.info("find $userId join $projectId by dept")
//            val departmentGroup = mutableListOf<ManagerRoleGroupInfo>()
//            // 若用户组添加的为组织,则无法通过userId直接获取到与用户的关系,需要拿到项目下用户组内组织,匹配用户组织信息
//            // iam对管理类接口有频率限制,此处需添加缓存
//            watcher.start("getAllMemberAndDept")
//            val projectMemberInfos = getProjectAllMember(projectId, null, null)?.results
//                ?: return null
//            watcher.start("findGroupIndexInfo")
//            projectMemberInfos.forEach {
//                val userDeptIds by lazy { deptService.getUserDeptInfo(userId) }
//                // 如果是用户组下是组织,才匹配用户是否在该组织下
//                if (it.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT) &&
//                    userDeptIds.contains(it.id)) {
//                    watcher.start("getUserDept")
//                    logger.info("$userId join $projectId by dept ${it.id} ")
//                    departmentGroup.add(
//                        ManagerRoleGroupInfo(
//                            it.name,
//                            "",
//                            it.id.toInt()
//                        )
//                    )
//                    return@forEach
//                }
//            }
//            return departmentGroup
//        } finally {
//            watcher.stop()
//            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 50)
//        }
//    }

    override fun checkUser(userId: String) {
        return
    }
    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionRoleMemberImpl::class.java)
    }
}
