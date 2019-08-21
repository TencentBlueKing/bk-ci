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

package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryCodeSvn
import com.tencent.devops.model.repository.tables.records.TRepositoryCodeSvnRecord
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class RepositoryCodeSvnDao {
    fun create(
        dslContext: DSLContext,
        repositoryId: Long,
        region: CodeSvnRegion?,
        projectName: String,
        userName: String,
        privateToken: String,
        svnType: String?
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            dslContext.insertInto(
                this,
                REPOSITORY_ID,
                REGION,
                PROJECT_NAME,
                USER_NAME,
                CREDENTIAL_ID,
                CREATED_TIME,
                UPDATED_TIME,
                SVN_TYPE
            )
                .values(
                    repositoryId,
                    region?.name ?: "",
                    projectName,
                    userName,
                    privateToken,
                    now,
                    now,
                    svnType ?: ""
                ).execute()
        }
    }

    private fun getOrNull(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeSvnRecord? {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.eq(repositoryId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeSvnRecord {
        return getOrNull(dslContext, repositoryId) ?: throw NotFoundException("Svn repository does not exists")
    }

    fun edit(
        dslContext: DSLContext,
        repositoryId: Long,
        region: CodeSvnRegion?,
        projectName: String,
        userName: String,
        credentialId: String,
        svnType: String?
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            dslContext.update(this)
                .set(REGION, region?.name ?: "")
                .set(PROJECT_NAME, projectName)
                .set(USER_NAME, userName)
                .set(UPDATED_TIME, now)
                .set(CREDENTIAL_ID, credentialId)
                .set(SVN_TYPE, svnType ?: "")
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        repositoryIds: Set<Long>
    ): Result<TRepositoryCodeSvnRecord> {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.`in`(repositoryIds))
                .fetch()
        }
    }
}