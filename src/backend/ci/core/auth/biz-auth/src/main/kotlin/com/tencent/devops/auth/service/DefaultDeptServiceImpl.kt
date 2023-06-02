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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo

class DefaultDeptServiceImpl : DeptService {

    override fun getDeptByLevel(level: Int, accessToken: String?, userId: String): DeptInfoVo? {
        return null
    }

    override fun getDeptByParent(parentId: Int, accessToken: String?, userId: String, pageSize: Int?): DeptInfoVo? {
        return null
    }

    override fun getUserAndDeptByName(
        name: String,
        accessToken: String?,
        userId: String,
        type: ManagerScopesEnum,
        exactLookups: Boolean?
    ): List<UserAndDeptInfoVo?> {
        return emptyList()
    }

    override fun getDeptUser(deptId: Int, accessToken: String?): List<String> {
        return emptyList()
    }

    override fun getUserParentDept(userId: String): Int {
        return 0
    }

    override fun getDeptByName(deptName: String, userId: String): DeptInfoVo? {
        return null
    }

    override fun getUserDeptInfo(userId: String): Set<String> {
        return emptySet()
    }

    override fun getUserInfo(userId: String, name: String): UserAndDeptInfoVo? =
        UserAndDeptInfoVo(
            id = 0,
            name = name,
            type = ManagerScopesEnum.USER
        )
}
