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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.v2.UserMessageType
import com.tencent.devops.gitci.v2.dao.GitUserMessageDao
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 用作事务存储需要附带用户消息通知的数据
 */
@Service
class GitCIEventSaveService @Autowired constructor(
    private val dslContext: DSLContext,
    private val userMessageDao: GitUserMessageDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val gitRequestEventDao: GitRequestEventDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIEventSaveService::class.java)
    }

    fun saveNotBuildEvent(
        userId: String,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String?,
        reasonDetail: String?,
        pipelineId: String?,
        filePath: String?,
        gitProjectId: Long
    ): Long {
        var messageId = -1L
        val event = gitRequestEventDao.get(dslContext = dslContext, id = eventId)
            ?: throw RuntimeException("can't find event $eventId")
        val messageTitle = if (event.objectKind == OBJECT_KIND_MERGE_REQUEST) {
            "Merge requests [!${event.mergeRequestId}] opened by ${event.userId}"
        } else {
            if (event.objectKind == OBJECT_KIND_MANUAL) {
                "Manual Triggered by ${event.userId}"
            } else {
                "Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
            }
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            messageId = gitRequestEventNotBuildDao.save(
                dslContext = context,
                eventId = eventId,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                reason = reason,
                reasonDetail = reasonDetail,
                pipelineId = pipelineId,
                filePath = filePath,
                gitProjectId = gitProjectId
            )
            userMessageDao.save(
                dslContext = context,
                userId = userId,
                messageType = UserMessageType.REQUEST,
                messageId = messageId.toString(),
                messageTitle = messageTitle
            )
        }
        return messageId
    }
}
