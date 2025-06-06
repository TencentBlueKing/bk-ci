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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.UserCopilotResource
import com.tencent.devops.repository.service.CopilotOpenTokenService
import com.tencent.devops.repository.service.RepositoryCopilotService
import com.tencent.devops.scm.enums.AISummaryRateType
import com.tencent.devops.scm.pojo.CodeGitCopilotSummary
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCopilotResourceImpl @Autowired constructor(
    private val copilotOpenTokenService: CopilotOpenTokenService,
    private val repositoryCopilotService: RepositoryCopilotService
) : UserCopilotResource {
    override fun getCopilotOpenToken(userId: String, refresh: Boolean?): Result<String> {
        return Result(copilotOpenTokenService.getAccessToken(userId, refresh ?: false))
    }

    override fun createSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): Result<CodeGitCopilotSummary> {
        return Result(
            repositoryCopilotService.createSummary(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = elementId
            )
        )
    }

    override fun getSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): Result<CodeGitCopilotSummary> {
        return Result(
            repositoryCopilotService.getSummary(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = elementId
            )
        )
    }

    override fun rateSummary(
        userId: String,
        projectName: String,
        processId: String,
        type: AISummaryRateType,
        feedback: String?
    ): Result<Boolean> {
        repositoryCopilotService.rateSummary(
            userId = userId,
            projectName = projectName,
            processId = processId,
            type = type,
            feedback = feedback
        )
        return Result(true)
    }
}
