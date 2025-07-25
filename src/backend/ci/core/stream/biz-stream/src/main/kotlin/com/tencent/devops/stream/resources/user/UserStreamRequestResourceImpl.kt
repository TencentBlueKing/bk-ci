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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.stream.api.user.UserStreamRequestResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.EventTypeConf
import com.tencent.devops.stream.pojo.StreamGitRequestHistory
import com.tencent.devops.stream.service.StreamRequestService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamRequestResourceImpl @Autowired constructor(
    private val streamRequestService: StreamRequestService,
    private val permissionService: StreamPermissionService
) : UserStreamRequestResource {
    override fun getMergeBuildList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<StreamGitRequestHistory>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(streamRequestService.getRequestList(userId, gitProjectId, page, pageSize))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    override fun getEventTypeConf(userId: String): Result<List<EventTypeConf>> {
        val eventTypeList = mutableListOf<EventTypeConf>()
        eventTypeList.let { e ->
            StreamGitObjectKind.values().forEach {
                e.add(
                    EventTypeConf(
                        id = it.name,
                        name = it.value.capitalize()
                    )
                )
            }
        }

        return Result(eventTypeList)
    }
}
