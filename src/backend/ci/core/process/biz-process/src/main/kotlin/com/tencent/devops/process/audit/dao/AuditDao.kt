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

package com.tencent.devops.process.audit.dao

import com.tencent.devops.model.process.tables.TAuditResource
import com.tencent.devops.model.process.tables.records.TAuditResourceRecord
import com.tencent.devops.process.pojo.audit.QueryAudit
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class AuditDao {

    fun create(
        dslContext: DSLContext,
        resourceType: String,
        resourceId: String,
        resourceName: String,
        userId: String,
        action: String,
        actionContent: String,
        projectId: String,
        id: Long? = null
    ): Long {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            val record = dslContext.insertInto(
                this,
                RESOURCE_TYPE,
                RESOURCE_ID,
                RESOURCE_NAME,
                USER_ID,
                ACTION,
                ACTION_CONTENT,
                CREATED_TIME,
                STATUS,
                PROJECT_ID,
                ID
            ).values(
                resourceType,
                resourceId,
                resourceName,
                userId,
                action,
                actionContent,
                LocalDateTime.now(),
                "1",
                projectId,
                id
            )
                .returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun listByResourceTye(
        dslContext: DSLContext,
        queryAudit: QueryAudit,
        offset: Int,
        limit: Int
    ): Result<TAuditResourceRecord> {
        return with(TAuditResource.T_AUDIT_RESOURCE) {
            dslContext.selectFrom(this)
                .where(generateFilterCondition(queryAudit))
                .orderBy(ID.desc()).offset(offset).limit(limit).fetch()
        }
    }

    fun countByResourceTye(dslContext: DSLContext, queryAudit: QueryAudit): Long {
        return with(TAuditResource.T_AUDIT_RESOURCE) {
            dslContext.selectCount()
                .from(this)
                .where(generateFilterCondition(queryAudit))
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun generateFilterCondition(queryAudit: QueryAudit): MutableList<Condition> {
        val query = mutableListOf<Condition>()
        with(TAuditResource.T_AUDIT_RESOURCE) {
            query.add(RESOURCE_TYPE.eq(queryAudit.resourceType))
            query.add(PROJECT_ID.eq(queryAudit.projectId))

            if (!queryAudit.resourceId.isNullOrBlank()) {
                query.add(RESOURCE_ID.eq(queryAudit.resourceId))
            }

            if (!queryAudit.userId.isNullOrBlank()) {
                query.add(USER_ID.eq(queryAudit.userId))
            }
            if (!queryAudit.resourceName.isNullOrBlank()) {
                query.add(
                    RESOURCE_NAME.like("%${queryAudit.resourceName}%")
                        .or(ACTION_CONTENT.like("%${queryAudit.resourceName}%")) // 针对改名的场景检索
                )
            }

            if (!queryAudit.startTime.isNullOrBlank() && !queryAudit.endTime.isNullOrBlank()) {
                val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val startTimeDateTime = LocalDateTime.parse(queryAudit.startTime, df)
                val endTimeDateTime = LocalDateTime.parse(queryAudit.endTime, df)
                query.add(CREATED_TIME.between(startTimeDateTime, endTimeDateTime))
            }
        }
        return query
    }
}
