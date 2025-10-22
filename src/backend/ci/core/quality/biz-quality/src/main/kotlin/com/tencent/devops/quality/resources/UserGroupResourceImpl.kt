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

package com.tencent.devops.quality.resources

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.UserGroupResource
import com.tencent.devops.quality.pojo.Group
import com.tencent.devops.quality.pojo.GroupCreate
import com.tencent.devops.quality.pojo.GroupSummaryWithPermission
import com.tencent.devops.quality.pojo.GroupUpdate
import com.tencent.devops.quality.pojo.GroupUsers
import com.tencent.devops.quality.pojo.ProjectGroupAndUsers
import com.tencent.devops.quality.service.QualityNotifyGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGroupResourceImpl @Autowired constructor(
    private val qualityNotifyGroupService: QualityNotifyGroupService
) : UserGroupResource {
    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<GroupSummaryWithPermission>> {
        checkParam(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = qualityNotifyGroupService.list(userId, projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun projectGroupAndUsers(userId: String, projectId: String): Result<List<ProjectGroupAndUsers>> {
        checkParam(userId, projectId)
        return Result(qualityNotifyGroupService.getProjectGroupAndUsers(userId, projectId))
    }

    @AuditEntry(actionId = ActionId.QUALITY_GROUP_CREATE)
    override fun create(userId: String, projectId: String, group: GroupCreate): Result<Boolean> {
        checkParam(userId, projectId)
        qualityNotifyGroupService.create(userId, projectId, group)
        return Result(true)
    }

    override fun get(userId: String, projectId: String, groupHashId: String): Result<Group> {
        checkParam(userId, projectId, groupHashId)
        return Result(qualityNotifyGroupService.get(userId, projectId, groupHashId))
    }

    override fun getUsers(userId: String, projectId: String, groupHashId: String): Result<GroupUsers> {
        checkParam(userId, projectId, groupHashId)
        return Result(qualityNotifyGroupService.getUsers(userId, projectId, groupHashId))
    }

    @AuditEntry(actionId = ActionId.QUALITY_GROUP_EDIT)
    override fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        qualityNotifyGroupService.edit(userId, projectId, groupHashId, group)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.QUALITY_GROUP_DELETE)
    override fun delete(userId: String, projectId: String, groupHashId: String): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        qualityNotifyGroupService.delete(userId, projectId, groupHashId)
        return Result(true)
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    fun checkParam(userId: String, projectId: String, groupHashId: String) {
        checkParam(userId, projectId)
        if (groupHashId.isBlank()) {
            throw ParamBlankException("Invalid groupHashId")
        }
    }
}
