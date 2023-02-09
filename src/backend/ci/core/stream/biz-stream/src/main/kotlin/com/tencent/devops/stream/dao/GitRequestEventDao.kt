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

package com.tencent.devops.stream.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.model.stream.tables.TGitRequestEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.pojo.ChangeYamlList
import com.tencent.devops.stream.pojo.GitRequestEvent
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitRequestEventDao {

    fun saveGitRequest(
        dslContext: DSLContext,
        event: GitRequestEvent
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
        scmType: ScmType? = ScmType.CODE_GIT,
        commitMsg: String? = null
    ): GitRequestEvent? {
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
                GitRequestEvent(
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
                        when (scmType) {
                            ScmType.CODE_GIT -> JsonUtil.to(record.event, GitEvent::class.java)
                            ScmType.GITHUB -> {
                                when (record.objectKind) {
                                    StreamObjectKind.PULL_REQUEST.value -> JsonUtil.to(
                                        record.event,
                                        GithubPullRequestEvent::class.java
                                    )
                                    StreamObjectKind.PUSH.value -> JsonUtil.to(
                                        record.event,
                                        GithubPushEvent::class.java
                                    )
                                    StreamObjectKind.TAG_PUSH.value -> JsonUtil.to(
                                        record.event,
                                        GithubPushEvent::class.java
                                    )
                                    else -> throw IllegalArgumentException(
                                        "${record.objectKind} in github load action not support yet"
                                    )
                                }
                            }
                            else -> TODO("对接其他Git平台时需要补充")
                        }
                    } catch (e: Exception) {
                        null
                    },
                    commitAuthorName = null,
                    gitProjectName = null,
                    changeYamlList = try {
                        JsonUtil.to(record.changeYamlList, object : TypeReference<List<ChangeYamlList>>() {})
                    } catch (ignore: Throwable) {
                        emptyList()
                    }
                )
            }
        }
    }

    fun getWithEvent(
        dslContext: DSLContext,
        id: Long,
        commitMsg: String? = null
    ): GitRequestEvent? {
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
                GitRequestEvent(
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
    ): List<GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .orderBy(ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
            val result = mutableListOf<GitRequestEvent>()
            records.forEach {
                result.add(
                    GitRequestEvent(
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

    fun updateChangeYamlList(
        dslContext: DSLContext,
        id: Long,
        changeYamlList: List<ChangeYamlList>
    ) {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            dslContext.update(this)
                .set(CHANGE_YAML_LIST, JsonUtil.toJson(changeYamlList, false))
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getMergeRequestList(
        dslContext: DSLContext,
        gitProjectId: Long,
        page: Int,
        pageSize: Int
    ): List<GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(OBJECT_KIND.eq(StreamGitObjectKind.MERGE_REQUEST.value))
                .orderBy(ID.desc())
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
            val result = mutableListOf<GitRequestEvent>()
            records.forEach {
                result.add(
                    GitRequestEvent(
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
     * 根据ID删除
     */
    fun deleteByIds(
        dslContext: DSLContext,
        ids: Collection<Long>
    ): Int {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            return dslContext.delete(this)
                .where(ID.`in`(ids)).execute()
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        id: Long
    ): Int {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            return dslContext.delete(this)
                .where(ID.eq(id)).execute()
        }
    }

    /**
     * 根据ID批量查询
     */
    fun getRequestsById(
        dslContext: DSLContext,
        requestIds: Set<Int>,
        hasEvent: Boolean
    ): List<GitRequestEvent> {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val records = dslContext.selectFrom(this)
                .where(ID.`in`(requestIds))
                .orderBy(ID.desc())
                .fetch()
            val result = mutableListOf<GitRequestEvent>()
            records.forEach {
                result.add(
                    GitRequestEvent(
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
                        gitEvent = null,
                        commitAuthorName = null,
                        gitProjectName = null
                    )
                )
            }
            return result
        }
    }

    fun getMinByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Long {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            return dslContext.select(DSL.min(ID))
                .from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getEventsByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        minId: Long,
        limit: Long
    ): Result<out Record>? {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            val conditions = mutableListOf<Condition>()
            conditions.add(GIT_PROJECT_ID.eq(gitProjectId))
            conditions.add(ID.ge(minId))
            return dslContext.select(ID).from(this)
                .where(conditions)
                .orderBy(ID.asc())
                .limit(limit)
                .fetch()
        }
    }

    fun getClearDeleteEventIdList(
        dslContext: DSLContext,
        gitProjectId: Long,
        eventIdList: List<Long>,
        gapDays: Long
    ): Result<out Record>? {
        with(TGitRequestEvent.T_GIT_REQUEST_EVENT) {
            return dslContext.select(ID).from(this)
                .where(
                    GIT_PROJECT_ID.eq(gitProjectId)
                        .and(CREATE_TIME.lt(LocalDateTime.now().minusDays(gapDays)))
                        .and(ID.`in`(eventIdList))
                )
                .fetch()
        }
    }
}
