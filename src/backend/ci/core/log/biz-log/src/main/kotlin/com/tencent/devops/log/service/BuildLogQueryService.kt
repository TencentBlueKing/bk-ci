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

package com.tencent.devops.log.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogLineNum
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.jmx.LogStorageBean
import javax.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "TooManyFunctions")
@Service
class BuildLogQueryService @Autowired constructor(
    private val logService: LogService,
    private val logStatusService: LogStatusService,
    private val indexService: IndexService,
    private val logStorageBean: LogStorageBean,
    private val logPermissionService: LogPermissionService
) {

    fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.queryInitLogs(
                buildId = buildId,
                debug = debug ?: false,
                logType = logType,
                subTag = subTag,
                tag = tag,
                jobId = jobId,
                executeCount = executeCount
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getInitLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int?,
        pageSize: Int?,
        subTag: String? = null
    ): Result<PageQueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.queryInitLogsPage(
                buildId = buildId,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                page = page ?: -1,
                pageSize = pageSize ?: -1
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getMoreLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        num: Int,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.queryLogsBetweenLines(
                buildId = buildId,
                num = num,
                fromStart = fromStart ?: true,
                start = start,
                end = end,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.queryLogsAfterLine(
                buildId = buildId,
                start = start,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getBeforeLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        end: Long,
        debug: Boolean?,
        logType: LogType?,
        size: Int?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.queryLogsBeforeLine(
                buildId = buildId,
                end = end,
                size = size,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getLogMode(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String,
        executeCount: Int?
    ): Result<QueryLogStatus> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        return Result(
            logStatusService.getStorageMode(
                buildId = buildId,
                tag = tag,
                executeCount = executeCount ?: 1
            )
        )
    }

    fun getLastLineNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<QueryLogLineNum> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val lastLineNum = indexService.getLastLineNum(buildId)
        val finished = logStatusService.isFinish(buildId, null, null, null, null)
        return Result(
            QueryLogLineNum(
                buildId = buildId,
                finished = finished,
                lastLineNum = lastLineNum
            )
        )
    }

    fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        fileName: String?,
        subTag: String? = null
    ): Response {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.DOWNLOAD)
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val result = logService.downloadLogs(
                pipelineId = pipelineId,
                buildId = buildId,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                fileName = fileName
            )
            success = true
            return result
        } finally {
            logStorageBean.download(System.currentTimeMillis() - startEpoch, success)
        }
    }

    fun getEndLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        size: Int,
        debug: Boolean?,
        logType: LogType?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<EndPageQueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.getEndLogsPage(
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    fun getBottomLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        logType: LogType?,
        size: Int?,
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId, AuthPermission.VIEW)
        val startEpoch = System.currentTimeMillis()
        var success = false
        val queryLogs = try {
            val result = logService.getBottomLogs(
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug ?: false,
                logType = logType,
                tag = tag,
                subTag = subTag,
                jobId = jobId,
                executeCount = executeCount,
                size = size
            )
            result.timeUsed = System.currentTimeMillis() - startEpoch
            success = logStatusSuccess(result.status)
            result
        } finally {
            logStorageBean.query(System.currentTimeMillis() - startEpoch, success)
        }
        return Result(queryLogs)
    }

    @Suppress("ThrowsCount")
    private fun validateAuth(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        permission: AuthPermission
    ) {
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
        if (!logPermissionService.verifyUserLogPermission(
                userId = userId,
                pipelineId = pipelineId,
                projectCode = projectId,
                permission = permission
            )
        ) {
            val language = I18nUtil.getLanguage(userId)
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language,
                    arrayOf(userId, projectId, AuthPermission.EDIT.getI18n(language), pipelineId)
                )
            )
        }
    }

    private fun logStatusSuccess(logStatus: Int): Boolean {
        return LogStatus.parse(logStatus) == LogStatus.EMPTY ||
            LogStatus.parse(logStatus) == LogStatus.SUCCEED
    }
}
