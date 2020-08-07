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

package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.ServiceLogResource
import com.tencent.devops.gitci.service.RepositoryConfService
import com.tencent.devops.gitci.service.LogService
import com.tencent.devops.common.log.pojo.QueryLogs
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ServiceLogResourceImpl @Autowired constructor(
    private val logService: LogService,
    private val repositoryConfService: RepositoryConfService
) : ServiceLogResource {
    override fun getInitLogs(gitProjectId: Long, buildId: String, isAnalysis: Boolean?, queryKeywords: String?, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getInitLogs(gitProjectId, buildId, isAnalysis, queryKeywords, tag, jobId, executeCount))
    }

    override fun getMoreLogs(gitProjectId: Long, buildId: String, num: Int?, fromStart: Boolean?, start: Long, end: Long, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getMoreLogs(gitProjectId, buildId, num, fromStart, start, end, tag, jobId, executeCount))
    }

    override fun getAfterLogs(gitProjectId: Long, buildId: String, start: Long, isAnalysis: Boolean?, queryKeywords: String?, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getAfterLogs(gitProjectId, buildId, start, isAnalysis, queryKeywords, tag, jobId, executeCount))
    }

    override fun downloadLogs(gitProjectId: Long, buildId: String, tag: String?, jobId: String?, executeCount: Int?): Response {
        checkParam(buildId, gitProjectId)
        return logService.downloadLogs(gitProjectId, buildId, tag, jobId, executeCount)
    }

    private fun checkParam(buildId: String, gitProjectId: Long) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (!repositoryConfService.initGitCISetting("", gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目无法开启工蜂CI，请联系蓝盾助手")
        }
    }
}