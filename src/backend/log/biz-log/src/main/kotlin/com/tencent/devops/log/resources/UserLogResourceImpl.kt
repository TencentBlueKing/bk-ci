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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.UserLogResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.PipelineLogService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

/**
 *
 * Powered By Tencent
 */
@RestResource
class UserLogResourceImpl @Autowired constructor(
    private val logService: PipelineLogService,
    private val indexService: IndexService,
    private val authPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : UserLogResource {

    override fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {

        validateAuth(userId, projectId, pipelineId, buildId)

        val indexAndType = indexService.parseIndexAndType(buildId)
        return Result(
            logService.queryInitLogs(
                buildId, indexAndType.left, indexAndType.right,
                isAnalysis ?: false, queryKeywords, tag, executeCount
            )
        )
    }

    override fun getMoreLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    )
        : Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)

        val indexAndType = indexService.parseIndexAndType(buildId)

        return Result(
            logService.queryMoreLogsBetweenLines(
                buildId,
                indexAndType.left,
                indexAndType.right,
                num ?: 100,
                fromStart ?: true,
                start,
                end,
                tag,
                executeCount
            )
        )
    }

    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    )
        : Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)

        val indexAndType = indexService.parseIndexAndType(buildId)

        return Result(
            logService.queryMoreLogsAfterLine(
                buildId, indexAndType.left, indexAndType.right, start, isAnalysis ?: false,
                queryKeywords, tag, executeCount
            )
        )
    }

    override fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ): Response {
        validateAuth(userId, projectId, pipelineId, buildId)
        return logService.downloadLogs(pipelineId, buildId, tag ?: "", executeCount)
    }

    private fun validateAuth(userId: String, projectId: String, pipelineId: String, buildId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = BkAuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = BkAuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看流水线")
        }
    }
}