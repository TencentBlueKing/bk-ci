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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.entity.SearchUserAndDeptEntity
import com.tencent.devops.auth.pojo.vo.BkDeptDetailsVo
import com.tencent.devops.auth.pojo.vo.BkUserInfoVo
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo

interface DeptService {
    fun getDeptByLevel(level: Int, accessToken: String?, userId: String): DeptInfoVo?

    fun getDeptByParent(parentId: Int, accessToken: String?, userId: String, pageSize: Int?): DeptInfoVo?

    fun getUserAndDeptByName(
        name: String,
        accessToken: String?,
        userId: String,
        type: ManagerScopesEnum,
        exactLookups: Boolean? = false
    ): List<UserAndDeptInfoVo?>

    fun getDeptUser(deptId: Int, accessToken: String?): List<String>?

    // 获取用户组织上一级组织
    fun getUserParentDept(userId: String): Int

    fun getDeptByName(deptName: String, userId: String): DeptInfoVo?

    fun getUserDeptInfo(userId: String): Set<String>

    @Deprecated("老接口，已废弃")
    fun getUserInfo(userId: String, name: String): UserAndDeptInfoVo?

    // 获取单个用户信息
    fun getUserInfo(userId: String): UserAndDeptInfoVo?

    // 获取成员信息
    fun getMemberInfo(
        memberId: String,
        memberType: ManagerScopesEnum
    ): UserAndDeptInfoVo

    // 获取成员信息
    fun listMemberInfos(
        memberIds: List<String>,
        memberType: ManagerScopesEnum
    ): List<UserAndDeptInfoVo>

    // 传入成员名单，筛选出其中离职的成员
    fun listDepartedMembers(
        memberIds: List<String>
    ): List<String>

    fun isUserDeparted(userId: String): Boolean

    fun listDeptInfos(searchUserEntity: SearchUserAndDeptEntity): DeptInfoVo

    fun listUserInfos(searchUserEntity: SearchUserAndDeptEntity): BkUserInfoVo

    fun getUserDeptDetails(userId: String): BkDeptDetailsVo?
}
