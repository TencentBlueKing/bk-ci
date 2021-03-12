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

package com.tencent.devops.experience.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.user.UserGroupResource
import com.tencent.devops.experience.pojo.Group
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.enums.ProjectGroup
import com.tencent.devops.experience.service.GroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGroupResourceImpl @Autowired constructor(private val groupService: GroupService) : UserGroupResource {
    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        returnPublic: Boolean?
    ): Result<Page<GroupSummaryWithPermission>> {
        checkParam(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = groupService.list(
            userId = userId,
            projectId = projectId,
            offset = offset,
            limit = pageSizeNotNull,
            returnPublic = returnPublic ?: false
        )
        return Result(
            Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = result.first,
                records = result.second
            )
        )
    }

    override fun getProjectUsers(userId: String, projectId: String, projectGroup: ProjectGroup?): Result<List<String>> {
        checkParam(userId, projectId)
        return Result(groupService.getProjectUsers(userId = userId, projectId = projectId, projectGroup = projectGroup))
    }

    override fun projectGroupAndUsers(userId: String, projectId: String): Result<List<ProjectGroupAndUsers>> {
        checkParam(userId, projectId)
        return Result(groupService.getProjectGroupAndUsers(userId = userId, projectId = projectId))
    }

    override fun create(userId: String, projectId: String, group: GroupCreate): Result<Boolean> {
        checkParam(userId, projectId)
        groupService.create(projectId = projectId, userId = userId, group = group)
        return Result(true)
    }

    override fun get(userId: String, projectId: String, groupHashId: String): Result<Group> {
        checkParam(userId, projectId, groupHashId)
        return Result(groupService.get(userId, projectId, groupHashId))
    }

    override fun getUsers(userId: String, projectId: String, groupHashId: String): Result<GroupUsers> {
        checkParam(userId, projectId, groupHashId)
        return Result(groupService.getUsers(userId = userId, projectId = projectId, groupHashId = groupHashId))
    }

    override fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        groupService.edit(userId = userId, projectId = projectId, groupHashId = groupHashId, group = group)
        return Result(true)
    }

    override fun delete(userId: String, projectId: String, groupHashId: String): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        groupService.delete(userId = userId, projectId = projectId, groupHashId = groupHashId)
        return Result(true)
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    fun checkParam(userId: String, projectId: String, groupHashId: String) {
        checkParam(userId, projectId)
        if (groupHashId.isBlank()) {
            throw ParamBlankException("Invalid groupHashId")
        }
    }
}
