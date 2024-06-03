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

package com.tencent.devops.process.audit.service

import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.audit.dao.AuditDao
import com.tencent.devops.process.pojo.audit.AuditInfo
import com.tencent.devops.process.pojo.audit.QueryAudit
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuditService @Autowired constructor(
    private val auditDao: AuditDao,
    private val dslContext: DSLContext,
    private val client: Client
) {

    fun createAudit(audit: com.tencent.devops.process.pojo.audit.Audit): Long {
        checkParam(audit)
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId("AUDIT_RESOURCE").data
        return auditDao.create(
            dslContext = dslContext,
            resourceType = audit.resourceType,
            resourceId = audit.resourceId,
            resourceName = audit.resourceName,
            userId = audit.userId,
            action = audit.action,
            actionContent = audit.actionContent,
            projectId = audit.projectId,
            id = id
        )
    }

    fun userList(
        queryAudit: QueryAudit,
        offset: Int,
        limit: Int
    ): Pair<SQLPage<AuditInfo>, Boolean> {
        val count = auditDao.countByResourceTye(dslContext = dslContext, queryAudit = queryAudit)
        val auditRecordList = auditDao.listByResourceTye(
            dslContext = dslContext,
            queryAudit = queryAudit,
            offset = offset,
            limit = limit
        )
        val auditRecordMap = auditRecordList.toSet()
        val auditList = auditRecordMap.map {
            val statusStr = if (it.status == "1") {
                SUCCESS
            } else {
                FAIL
            }
            AuditInfo(
                status = I18nUtil.getCodeLanMessage(statusStr),
                resourceType = it.resourceType,
                resourceId = it.resourceId,
                resourceName = it.resourceName,
                userId = it.userId,
                updatedTime = it.createdTime.timestamp(),
                action = it.action,
                actionContent = it.actionContent
            )
        }
        return Pair(SQLPage(count, auditList), true)
    }

    private fun checkParam(audit: com.tencent.devops.process.pojo.audit.Audit) {
        if (audit.resourceType.isBlank()) {
            throw ParamBlankException("Invalid resourceType")
        }
        if (audit.resourceId.isBlank()) {
            throw ParamBlankException("Invalid resourceId")
        }
        if (audit.resourceName.isBlank()) {
            throw ParamBlankException("Invalid resourceName")
        }
        if (audit.userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (audit.action.isBlank()) {
            throw ParamBlankException("Invalid action")
        }
        if (audit.actionContent.isBlank()) {
            throw ParamBlankException("Invalid actionContent")
        }
    }
}
