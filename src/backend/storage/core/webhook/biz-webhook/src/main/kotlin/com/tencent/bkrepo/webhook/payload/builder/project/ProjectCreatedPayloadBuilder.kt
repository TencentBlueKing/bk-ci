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

package com.tencent.bkrepo.webhook.payload.builder.project

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.webhook.exception.WebHookMessageCode
import com.tencent.bkrepo.webhook.payload.builder.EventPayloadBuilder
import com.tencent.bkrepo.webhook.pojo.payload.project.ProjectCreatedEventPayload
import org.springframework.stereotype.Component

@Component
class ProjectCreatedPayloadBuilder(
    private val projectClient: ProjectClient
) : EventPayloadBuilder(
    eventType = EventType.PROJECT_CREATED
) {

    override fun build(event: ArtifactEvent): ProjectCreatedEventPayload {
        return ProjectCreatedEventPayload(
            user = getUser(event.userId),
            project = getProject(event.projectId)
        )
    }

    private fun getProject(projectId: String): ProjectInfo {
        return projectClient.getProjectInfo(projectId).data
            ?: throw ErrorCodeException(WebHookMessageCode.WEBHOOK_PROJECT_NOT_FOUND)
    }
}
