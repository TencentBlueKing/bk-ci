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

package com.tencent.devops.stream.v1.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.model.stream.tables.TGitRequestEvent
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class V1GitRequestEventDao {

    fun saveGitRequest(
        dslContext: DSLContext,
        event: V1GitRequestEvent
    ): Long {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val record = dslContext.insertInto(
                this,
                OBJECT_KIND,
                OPERATION_KIND,
                EXTENSION_ACTION,
                GIT_PROJECT_ID,
                SOURCE_GIT_PROJECT_ID,
                BRANCH,
                TARGET_BRANCH,
                COMMIT_ID,
                COMMIT_MESSAGE,
                COMMIT_TIMESTAMP,
                USER_NAME,
                TOTAL_COMMIT_COUNT,
                MERGE_REQUEST_ID,
                EVENT,
                CREATE_TIME,
                MR_TITLE
            ).values(
                event.objectKind,
                event.operationKind,
                event.extensionAction,
                event.gitProjectId,
                event.sourceGitProjectId,
                event.branch,
                event.targetBranch,
                event.commitId,
                CommonUtils.interceptStringInLength(event.commitMsg, 1000),
                event.commitTimeStamp,
                event.userId,
                event.totalCommitCount,
                event.mergeRequestId,
                event.event,
                LocalDateTime.now(),
                event.mrTitle
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun get(
        dslContext: DSLContext,
        id: Long,
        commitMsg: String? = null
    ): V1GitRequestEvent? {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val dsl = dslContext.selectFrom(this)
                .where(ID.eq(id))
            if (!commitMsg.isNullOrBlank()) {
                dsl.and(COMMIT_MESSAGE.like("%$commitMsg%"))
            }
            val record = dsl.fetchAny()
            return if (record == null) {
                null
            } else {
                V1GitRequestEvent(
                    id = record.id,
                    objectKind = record.objectKind,
                    operationKind = record.operationKind,
                    extensionAction = record.extensionAction,
                    gitProjectId = record.gitProjectId,
                    sourceGitProjectId = record.sourceGitProjectId,
                    branch = record.branch,
                    targetBranch = record.targetBranch,
                    commitId = record.commitId,
                    commitMsg = record.commitMessage,
                    commitTimeStamp = record.commitTimestamp,
                    userId = record.userName,
                    totalCommitCount = record.totalCommitCount,
                    mergeRequestId = record.mergeRequestId,
                    event = "", // record.event,
                    description = if (record.description.isNullOrBlank()) {
                        record.commitMessage
                    } else {
                        record.description
                    },
                    mrTitle = record.mrTitle,
                    gitEvent = try {
                        JsonUtil.to(record.event, GitEvent::class.java)
                    } catch (e: Exception) {
                        null
                    },
                    commitAuthorName = null,
                    gitProjectName = null
                )
            }
        }
    }

    fun getWithEvent(
        dslContext: DSLContext,
        id: Long,
        commitMsg: String? = null
    ): V1GitRequestEvent? {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val dsl = dslContext.selectFrom(this)
                .where(ID.eq(id))
            if (commitMsg != null) {
                dsl.and(COMMIT_MESSAGE.like("%$commitMsg%"))
            }
            val record = dsl.fetchAny()
            return if (record == null) {
                null
            } else {
                V1GitRequestEvent(
                    id = record.id,
                    objectKind = record.objectKind,
                    operationKind = record.operationKind,
                    extensionAction = record.extensionAction,
                    gitProjectId = record.gitProjectId,
                    sourceGitProjectId = record.sourceGitProjectId,
                    branch = record.branch,
                    targetBranch = record.targetBranch,
                    commitId = record.commitId,
                    commitMsg = record.commitMessage,
                    commitTimeStamp = record.commitTimestamp,
                    userId = record.userName,
                    totalCommitCount = record.totalCommitCount,
                    mergeRequestId = record.mergeRequestId,
                    event = record.event,
                    description = if (record.description.isNullOrBlank()) {
                        record.commitMessage
                    } else {
                        record.description
                    },
                    mrTitle = record.mrTitle,
                    gitEvent = null,
                    commitAuthorName = null,
                    gitProjectName = null
                )
            }
        }
    }

    fun getRequestList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int,
        pageSize: Int
    ): List<V1GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .orderBy(ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
            val result = mutableListOf<V1GitRequestEvent>()
            records.forEach {
                result.add(
                    V1GitRequestEvent(
                        id = it.id,
                        objectKind = it.objectKind,
                        operationKind = it.operationKind,
                        extensionAction = it.extensionAction,
                        gitProjectId = it.gitProjectId,
                        sourceGitProjectId = it.sourceGitProjectId,
                        branch = it.branch,
                        targetBranch = it.targetBranch,
                        commitId = it.commitId,
                        commitMsg = it.commitMessage,
                        commitTimeStamp = it.commitTimestamp,
                        userId = it.userName,
                        totalCommitCount = it.totalCommitCount,
                        mergeRequestId = it.mergeRequestId,
                        event = "", // record.event,
                        description = if (it.description.isNullOrBlank()) {
                            it.commitMessage
                        } else {
                            it.description
                        },
                        mrTitle = it.mrTitle,
                        gitEvent = null,
                        commitAuthorName = null,
                        gitProjectName = null
                    )
                )
            }
            return result
        }
    }

    fun getMergeRequestList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int,
        pageSize: Int
    ): List<V1GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(OBJECT_KIND.eq(StreamGitObjectKind.MERGE_REQUEST.value))
                .orderBy(ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
            val result = mutableListOf<V1GitRequestEvent>()
            records.forEach {
                result.add(
                    V1GitRequestEvent(
                        id = it.id,
                        objectKind = it.objectKind,
                        operationKind = it.operationKind,
                        extensionAction = it.extensionAction,
                        gitProjectId = it.gitProjectId,
                        sourceGitProjectId = it.sourceGitProjectId,
                        branch = it.branch,
                        targetBranch = it.targetBranch,
                        commitId = it.commitId,
                        commitMsg = it.commitMessage,
                        commitTimeStamp = it.commitTimestamp,
                        userId = it.userName,
                        totalCommitCount = it.totalCommitCount,
                        mergeRequestId = it.mergeRequestId,
                        event = "", // record.event,
                        description = if (it.description.isNullOrBlank()) {
                            it.commitMessage
                        } else {
                            it.description
                        },
                        mrTitle = it.mrTitle,
                        gitEvent = null,
                        commitAuthorName = null,
                        gitProjectName = null
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
                .fetchOne(0, Long::class.java)!!
        }
    }

    /**
     * 根据ID批量查询
     */
    fun getRequestsById(
        dslContext: DSLContext,
        requestIds: Set<Int>,
        hasEvent: Boolean
    ): List<V1GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(ID.`in`(requestIds))
                .orderBy(ID.desc())
                .fetch()
            val result = mutableListOf<V1GitRequestEvent>()
            records.forEach {
                result.add(
                    V1GitRequestEvent(
                        id = it.id,
                        objectKind = it.objectKind,
                        operationKind = it.operationKind,
                        extensionAction = it.extensionAction,
                        gitProjectId = it.gitProjectId,
                        sourceGitProjectId = it.sourceGitProjectId,
                        branch = it.branch,
                        targetBranch = it.targetBranch,
                        commitId = it.commitId,
                        commitMsg = it.commitMessage,
                        commitTimeStamp = it.commitTimestamp,
                        userId = it.userName,
                        totalCommitCount = it.totalCommitCount,
                        mergeRequestId = it.mergeRequestId,
                        event = if (hasEvent) {
                            it.event
                        } else {
                            ""
                        },
                        description = if (it.description.isNullOrBlank()) {
                            it.commitMessage
                        } else {
                            it.description
                        },
                        mrTitle = it.mrTitle,
                        gitEvent = try {
                            JsonUtil.to(it.event, GitEvent::class.java)
                        } catch (e: Exception) {
                            null
                        },
                        commitAuthorName = null,
                        gitProjectName = null
                    )
                )
            }
            return result
        }
    }
}
