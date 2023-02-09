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

package com.tencent.devops.remotedev.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.remotedev.dao.WorkspaceDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
class PermissionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao
) {
    @Value("\${remoteDev.enablePermission:true}")
    private val enablePermission: Boolean = true

    private val projectUserCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, List<String>>() {
                override fun load(name: String): List<String> {
                    return workspaceDao.fetchWorkspaceUser(dslContext, name)
                }
            }
        )

    fun checkPermission(userId: String, workspaceName: String) {
        if (!enablePermission) return

        if (!projectUserCache.get(workspaceName).contains(userId)) {
            throw CustomException(Response.Status.FORBIDDEN, "拒绝访问")
        }
    }

    fun checkUserPermission(userId: String, workspaceName: String): Boolean {
        if (!enablePermission) return true

        if (!projectUserCache.get(workspaceName).contains(userId)) {
            return false
        }
        return true
    }
}
