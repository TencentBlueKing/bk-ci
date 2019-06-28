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

package com.tencent.devops.ticket.dao

import com.tencent.devops.model.ticket.tables.TCredential
import com.tencent.devops.model.ticket.tables.records.TCredentialRecord
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class CredentialDao {
    fun has(dslContext: DSLContext, projectId: String, credentialId: String): Boolean {
        with(TCredential.T_CREDENTIAL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREDENTIAL_ID.eq(credentialId))
                .fetchOne() != null
        }
    }

    fun get(dslContext: DSLContext, projectId: String, credentialId: String): TCredentialRecord {
        return getOrNull(dslContext, projectId, credentialId)
            ?: throw NotFoundException("credentialId: $credentialId does not exist")
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, credentialId: String): TCredentialRecord? {
        with(TCredential.T_CREDENTIAL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREDENTIAL_ID.eq(credentialId))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        credentialUserId: String,
        credentialId: String,
        credentialType: String,
        credentialV1: String,
        credentialV2: String?,
        credentialV3: String?,
        credentialV4: String?,
        credentialRemark: String?
    ) {
        val now = LocalDateTime.now()
        with(TCredential.T_CREDENTIAL) {
            dslContext
                .insertInto(
                    this,
                    PROJECT_ID,
                    CREDENTIAL_USER_ID,
                    CREDENTIAL_ID,
                    CREDENTIAL_TYPE,
                    CREDENTIAL_V1,
                    CREDENTIAL_V2,
                    CREDENTIAL_V3,
                    CREDENTIAL_V4,
                    CREDENTIAL_REMARK,
                    CREATED_TIME,
                    UPDATED_TIME
                )
                .values(
                    projectId,
                    credentialUserId,
                    credentialId,
                    credentialType,
                    credentialV1,
                    credentialV2,
                    credentialV3,
                    credentialV4,
                    credentialRemark,
                    now,
                    now
                )
                .execute()
        }
    }

    fun updateIgnoreNull(
        dslContext: DSLContext,
        projectId: String,
        credentialId: String,
        credentialV1: String?,
        credentialV2: String?,
        credentialV3: String?,
        credentialV4: String?,
        credentialRemark: String?
    ) {
        val now = LocalDateTime.now()
        with(TCredential.T_CREDENTIAL) {
            val updateStep = dslContext.update(this)
            if (credentialV1 != null) updateStep.set(CREDENTIAL_V1, credentialV1)
            if (credentialV2 != null) updateStep.set(CREDENTIAL_V2, credentialV2)
            if (credentialV3 != null) updateStep.set(CREDENTIAL_V3, credentialV3)
            if (credentialV4 != null) updateStep.set(CREDENTIAL_V4, credentialV4)
            updateStep.set(CREDENTIAL_REMARK, credentialRemark)
                .set(UPDATED_TIME, now)
                .where(PROJECT_ID.eq(projectId))
                .and(CREDENTIAL_ID.eq(credentialId))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, credentialId: String) {
        with(TCredential.T_CREDENTIAL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREDENTIAL_ID.eq(credentialId))
                .execute()
        }
    }

    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        credentialTypes: Set<CredentialType>?,
        credentialIds: Set<String>,
        offset: Int,
        limit: Int
    ): Result<TCredentialRecord> {
        val credentialTypeStrings = credentialTypes?.map {
            it.name
        }
        return with(TCredential.T_CREDENTIAL) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))

            if (credentialTypeStrings != null) {
                query.and(CREDENTIAL_TYPE.`in`(credentialTypeStrings))
            }

            if (credentialIds.isNotEmpty()) {
                query.and(CREDENTIAL_ID.`in`(credentialIds))
            }
            query.orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listByProject(dslContext: DSLContext, projectId: String, offset: Int, limit: Int): Result<TCredentialRecord> {
        with(TCredential.T_CREDENTIAL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun countByProject(dslContext: DSLContext, projectId: String): Long {
        with(TCredential.T_CREDENTIAL) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, kotlin.Long::class.java)
        }
    }

    fun countByProject(
        dslContext: DSLContext,
        projectId: String,
        credentialTypes: Set<CredentialType>?,
        credentialIds: Set<String>
    ): Long {
        val credentialTypeStrings = credentialTypes?.map {
            it.name
        }
        return with(TCredential.T_CREDENTIAL) {
            val query = dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))

            if (credentialTypeStrings != null) {
                query.and(CREDENTIAL_TYPE.`in`(credentialTypeStrings))
            }

            if (credentialIds.isNotEmpty()) {
                query.and(CREDENTIAL_ID.`in`(credentialIds))
            }
            query.fetchOne(0, kotlin.Long::class.java)
        }
    }
}
