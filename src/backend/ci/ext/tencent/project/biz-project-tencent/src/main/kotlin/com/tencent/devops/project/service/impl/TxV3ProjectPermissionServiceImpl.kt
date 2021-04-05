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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired

class TxV3ProjectPermissionServiceImpl @Autowired constructor (
    val iamEsbService: IamEsbService
): ProjectPermissionService {



    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun createResources(userId: String, accessToken: String?, resourceRegisterInfo: ResourceRegisterInfo, userDeptDetail: UserDeptDetail?): String {
        /**
         *  V3创建项目流程
         *  1. 创建分级管理员，并记录iam分级管理员id
         *  2. 添加创建人到该分级管理员
         *  3. 添加默认用户组”CI管理员“
         *  4. 分配”ALL action“权限到CI管理员
         *  5. 添加创建人到分级管理员
         */

    }

    override fun deleteResource(projectCode: String) {
        TODO("Not yet implemented")
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        TODO("Not yet implemented")
    }

    override fun getUserProjects(userId: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        TODO("Not yet implemented")
    }

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String, permission: AuthPermission): Boolean {
        TODO("Not yet implemented")
    }
}
