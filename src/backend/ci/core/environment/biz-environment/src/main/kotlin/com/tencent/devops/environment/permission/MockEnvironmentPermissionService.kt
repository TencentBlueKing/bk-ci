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

package com.tencent.devops.environment.permission

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import org.jooq.DSLContext

/**
 * 权限校验接口
 */
class MockEnvironmentPermissionService constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    environmentAuthServiceCode: EnvironmentAuthServiceCode
) : AbstractEnvironmentPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    environmentAuthServiceCode = environmentAuthServiceCode
) {

    override fun supplierForEnvFakePermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            envDao.list(
                dslContext = dslContext,
                projectId = projectId
            ).forEach {
                fakeList.add(HashUtil.encodeLongId(it.envId))
            }
            fakeList
        }
    }

    override fun supplierForNodeFakePermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            nodeDao.listNodes(
                dslContext = dslContext,
                projectId = projectId
            ).forEach {
                fakeList.add(HashUtil.encodeLongId(it.nodeId))
            }
            fakeList
        }
    }
}
