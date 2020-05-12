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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.audit.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.audit.api.UserAuditResource
import com.tencent.devops.audit.api.pojo.AuditInfo
import com.tencent.devops.audit.api.pojo.AuditPage
import com.tencent.devops.audit.service.AuditService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuditResourceImpl @Autowired constructor(
        private val auditService: AuditService
) : UserAuditResource {

    companion object {
        private const val PageSize = 20
    }



    override fun list(
        userId: String?,
        projectId: String,
        resourceType: String,
        status: String?,
        resourceName: String?,
        startTime: String?,
        endTime: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AuditPage<AuditInfo>> {
        if (userId!=null && userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }

        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageSize
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = auditService.userList(userId, projectId, resourceType,status, resourceName,startTime,endTime,limit.offset, limit.limit)
        return Result(
                AuditPage(
                pageNotNull,
                pageSizeNotNull,
                result.first.count,
                result.first.records,
                result.second
            )
        )
    }


}