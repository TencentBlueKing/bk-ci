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

package com.tencent.devops.process.api.report

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.util.RegexUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserReportResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.report.service.ReportService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNUSED")
@RestResource
class UserReportResourceImpl @Autowired constructor(
    private val reportService: ReportService
) : UserReportResource {

    override fun get(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String?
    ): Result<List<Report>> {
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
        val result = reportService.list(userId, projectId, pipelineId, buildId, taskId)
        val decorateResult = mutableListOf<Report>()
        result.forEach {
            if (it.type == ReportTypeEnum.INTERNAL.name) {
                val httpContextPath = RegexUtils.splitDomainContextPath(it.indexFileUrl) // #4796 用户界面只保留contextPath
                if (httpContextPath != null) {
                    decorateResult.add(it.copy(indexFileUrl = httpContextPath.second))
                } else {
                    decorateResult.add(it)
                }
            } else {
                decorateResult.add(it)
            }
        }
        return Result(decorateResult)
    }

    override fun getStream(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<Report>> {
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
        val result = reportService.listNoApiHost(userId, projectId, pipelineId, buildId)
        return Result(result)
    }
}
