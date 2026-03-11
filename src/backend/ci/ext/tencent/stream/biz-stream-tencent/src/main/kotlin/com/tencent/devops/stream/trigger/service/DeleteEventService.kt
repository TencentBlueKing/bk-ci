/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.stream.trigger.service

import com.tencent.devops.stream.dao.DeleteEventDao
import com.tencent.devops.stream.pojo.StreamDeleteEvent
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeleteEventService @Autowired constructor(
    private val dslContext: DSLContext,
    private val deleteEventDao: DeleteEventDao
) {

    fun saveDeleteEvent(
        streamDeleteEvent: StreamDeleteEvent
    ) {
        deleteEventDao.save(dslContext, streamDeleteEvent)
    }

    fun getDeleteEvent(
        pipelineId: String
    ): StreamDeleteEvent? {
        val result = deleteEventDao.get(dslContext, pipelineId) ?: return null
        return with(result) {
            StreamDeleteEvent(
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                userId = creator,
                eventId = eventId,
                originYaml = originYaml
            )
        }
    }

    fun listDeleteEvent(
        gitProjectId: Long
    ): List<StreamDeleteEvent> {
        val result = deleteEventDao.list(dslContext, gitProjectId).ifEmpty { return emptyList() }
        return result.map {
            StreamDeleteEvent(
                gitProjectId = gitProjectId,
                pipelineId = it.pipelineId,
                userId = it.creator,
                eventId = it.eventId,
                originYaml = it.originYaml
            )
        }
    }

    fun deleteDeleteEvent(
        pipelineId: String
    ): Boolean {
        return deleteEventDao.delete(dslContext, pipelineId) > 0
    }
}
