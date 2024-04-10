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

package com.tencent.devops.statistics.service.project.impl

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.statistics.service.project.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StatProjectPermissionServiceImpl @Autowired constructor(
    private val client: Client,
    private val bkTag: BkTag,
    private val tokenService: ClientTokenService
) : ProjectPermissionService {

    @Value("\${tag.rbac:#{null}}")
    private var rbacTag: String = ""

    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    override fun getUserProjects(userId: String): List<String> {
        val projectList = mutableListOf<String>()
        getIamProjectList(tag = rbacTag, projectList = projectList, userId = userId)
        getIamProjectList(tag = prodTag, projectList = projectList, userId = userId)
        return projectList
    }

    private fun getIamProjectList(
        tag: String?,
        projectList: MutableList<String>,
        userId: String
    ): List<String> {
        if (!tag.isNullOrBlank()) {
            val iamProjectList = bkTag.invokeByTag(tag) {
                client.getGateway(ServiceProjectAuthResource::class).getUserProjects(
                    userId = userId,
                    token = tokenService.getSystemToken()!!
                ).data
            }
            if (iamProjectList != null) {
                projectList.addAll(iamProjectList)
            }
        }
        return projectList
    }
}
