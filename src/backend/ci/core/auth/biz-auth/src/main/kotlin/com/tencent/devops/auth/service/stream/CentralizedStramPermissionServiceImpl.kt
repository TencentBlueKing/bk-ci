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

package com.tencent.devops.auth.service.stream

import com.tencent.devops.auth.utils.GitTypeUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired

class CentralizedStramPermissionServiceImpl @Autowired constructor(
    val client: Client
) : StreamPermissionServiceImpl() {
    override fun isPublicProject(projectCode: String, userId: String?): Boolean {
        val gitType = GitTypeUtils.getType()
        // type: github, gitlab, svn, tgitd等
        // TODO: 根据不同的类型调用不同的代码源接口
        return true
    }

    override fun isProjectMember(projectCode: String, userId: String): Pair<Boolean, Boolean> {
        val gitType = GitTypeUtils.getType()
        // TODO: 根据不同的类型调用不同的代码源接口
        return Pair(true, true)
    }

    override fun extPermission(
        projectCode: String,
        userId: String,
        action: AuthPermission,
        resourceType: String
    ): Boolean {
        return false
    }
}
