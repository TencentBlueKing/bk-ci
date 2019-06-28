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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.OpLogResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.PipelineLogService
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 * Powered By Tencent
 */
@RestResource
class OpLogResourceImpl @Autowired constructor(
    val indexService: IndexService,
    val logService: PipelineLogService
) : OpLogResource {

    override fun preCreateIndices(numDays: Int): Result<Int> {
        if (numDays <= 0 || numDays > 1000) {
            throw IllegalArgumentException("无效的 numDays")
        }
        return Result(logService.preCreateIndices(numDays))
    }

    override fun createLogStatus(): Result<Boolean> {
        return Result(logService.createLogStatusIndex())
    }

    override fun getInitLogs(
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {

        val indexAndType = indexService.parseIndexAndType(buildId)

        return Result(
            logService.queryInitLogs(
                buildId, indexAndType.left, indexAndType.right, isAnalysis ?: false,
                queryKeywords, tag, executeCount
            )
        )
    }

    override fun getMoreLogs(
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val indexAndType = indexService.parseIndexAndType(buildId)

        return Result(
            logService.queryMoreLogsBetweenLines(
                buildId, indexAndType.left, indexAndType.right, num ?: 100,
                fromStart ?: true, start, end, tag, executeCount
            )
        )
    }

    override fun getAfterLogs(
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        val indexAndType = indexService.parseIndexAndType(buildId)

        return Result(
            logService.queryMoreLogsAfterLine(
                buildId, indexAndType.left, indexAndType.right, start,
                isAnalysis ?: false, queryKeywords, tag, executeCount
            )
        )
    }
}