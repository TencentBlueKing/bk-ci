/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.gitci.dao

import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.model.gitci.tables.TGitRequestEvent
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitRequestEventDao {

    fun saveGitRequest(
        dslContext: DSLContext,
        event: GitRequestEvent
    ): Long {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val record = dslContext.insertInto(this,
                    OBJECT_KIND,
                    OPERATION_KIND,
                    EXTENSION_ACTION,
                    GIT_PROJECT_ID,
                    BRANCH,
                    TARGET_BRANCH,
                    COMMIT_ID,
                    COMMIT_MSG,
                    COMMIT_TIMESTAMP,
                    USER_NAME,
                    TOTAL_COMMIT_COUNT,
                    MERGE_REQUEST_ID,
                    EVENT,
                    CREATE_TIME,
                    DESCRIPTION
                ).values(
                    event.objectKind,
                    event.operationKind,
                    event.extensionAction,
                    event.gitProjectId,
                    event.branch,
                    event.targetBranch,
                    event.commitId,
                    event.commitMsg,
                    event.commitTimeStamp,
                    event.userId,
                    event.totalCommitCount,
                    event.mergeRequestId,
                    event.event,
                    LocalDateTime.now(),
                    event.description
            ).returning(ID)
            .fetchOne()
            return record.id
        }
    }

    fun get(
        dslContext: DSLContext,
        id: Long
    ): GitRequestEvent? {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val record = dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
            return if (record == null) {
                null
            } else {
                GitRequestEvent(
                        record.id,
                        record.objectKind,
                        record.operationKind,
                        record.extensionAction,
                        record.gitProjectId,
                        record.branch,
                        record.targetBranch,
                        record.commitId,
                        record.commitMsg,
                        record.commitTimestamp,
                        record.userName,
                        record.totalCommitCount,
                        record.mergeRequestId,
                        "", // record.event,
                        record.description
                )
            }
        }
    }

    fun getRequestList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int,
        pageSize: Int
    ): List<GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .orderBy(ID.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
            val result = mutableListOf<GitRequestEvent>()
            records.forEach {
                result.add(GitRequestEvent(
                            it.id,
                            it.objectKind,
                            it.operationKind,
                            it.extensionAction,
                            it.gitProjectId,
                            it.branch,
                            it.targetBranch,
                            it.commitId,
                            it.commitMsg,
                            it.commitTimestamp,
                            it.userName,
                            it.totalCommitCount,
                            it.mergeRequestId,
                            "", // record.event,
                            it.description
                        )
                )
            }
            return result
        }
    }

    fun getRequestCount(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Long {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            return dslContext.selectCount()
                    .from(this)
                    .where(GIT_PROJECT_ID.eq(gitProjectId))
                    .orderBy(ID.desc())
                    .fetchOne(0, Long::class.java)
        }
    }
}
