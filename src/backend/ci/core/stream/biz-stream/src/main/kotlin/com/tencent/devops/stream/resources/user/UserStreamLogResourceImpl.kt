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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.user.UserStreamLogResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.service.StreamLogService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserStreamLogResourceImpl @Autowired constructor(
    private val streamLogService: StreamLogService,
    private val permissionService: StreamPermissionService
) : UserStreamLogResource {
    override fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(buildId)
        return Result(
            streamLogService.getInitLogs(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug,
                tag = tag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(buildId)
        return Result(
            streamLogService.getAfterLogs(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = buildId,
                start = start,
                debug = debug,
                tag = tag,
                jobId = jobId,
                executeCount = executeCount
            )
        )
    }

    override fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?
    ): Response {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(buildId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return streamLogService.downloadLogs(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    private fun checkParam(buildId: String) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
    }
}
