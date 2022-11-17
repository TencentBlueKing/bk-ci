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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.user.UserStreamUserMessageResource
import com.tencent.devops.stream.pojo.message.UserMessageRecord
import com.tencent.devops.stream.pojo.message.UserMessageType
import com.tencent.devops.stream.service.StreamUserMessageService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamUserMessageResourceImpl @Autowired constructor(
    private val streamUserMessageService: StreamUserMessageService
) : UserStreamUserMessageResource {
    override fun getUserMessages(
        userId: String,
        projectId: String?,
        messageType: UserMessageType?,
        haveRead: Boolean?,
        messageId: String?,
        triggerUserId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<UserMessageRecord>> {
        return Result(
            streamUserMessageService.getMessages(
                projectId = projectId,
                userId = userId,
                messageType = messageType,
                haveRead = haveRead,
                messageId = messageId?.ifBlank { null },
                triggerUserId = triggerUserId?.ifBlank { null },
                page = page ?: 1,
                pageSize = pageSize ?: 10
            )
        )
    }

    override fun getUserMessagesNoreadCount(userId: String, projectId: String?): Result<Int> {
        return Result(data = streamUserMessageService.getNoReadMessageCount(projectId, userId))
    }

    override fun readMessage(userId: String, id: Int, projectCode: String?): Result<Boolean> {
        return Result(streamUserMessageService.readMessage(userId = userId, id = id, projectCode = projectCode))
    }

    override fun readAllMessages(userId: String, projectCode: String?): Result<Boolean> {
        return Result(streamUserMessageService.readAllMessage(projectCode, userId = userId))
    }
}
