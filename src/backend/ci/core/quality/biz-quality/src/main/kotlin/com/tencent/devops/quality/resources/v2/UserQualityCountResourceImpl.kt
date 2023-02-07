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

package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.UserQualityCountResource
import com.tencent.devops.common.quality.pojo.QualityRuleIntercept
import com.tencent.devops.quality.api.v2.pojo.response.CountOverviewResponse
import com.tencent.devops.quality.pojo.CountDailyIntercept
import com.tencent.devops.quality.pojo.CountPipelineIntercept
import com.tencent.devops.quality.service.v2.QualityCountService
import com.tencent.devops.quality.service.v2.QualityHistoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityCountResourceImpl @Autowired constructor(
    private val countService: QualityCountService,
    private val historyService: QualityHistoryService
) : UserQualityCountResource {
    override fun getOverview(userId: String, projectId: String): Result<CountOverviewResponse> {
        return Result(countService.getOverview(userId, projectId))
    }

    override fun getPipelineIntercept(userId: String, projectId: String): Result<List<CountPipelineIntercept>> {
        return Result(countService.getPipelineIntercept(userId, projectId))
    }

    override fun getDailyIntercept(userId: String, projectId: String): Result<List<CountDailyIntercept>> {
        return Result(countService.getDailyIntercept(userId, projectId))
    }

    override fun getRuleIntercept(userId: String, projectId: String, page: Int?, pageSize: Int?): Result<Page<QualityRuleIntercept>> {
        checkParams(userId, projectId)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = page ?: 10
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = historyService.userGetRuleIntercept(userId, projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageNotNull, result.first, result.second))
    }

    private fun checkParams(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
