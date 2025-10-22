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
package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.store.api.common.UserStoreLogResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.common.service.StoreLogService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStoreLogResourceImpl @Autowired constructor(
    private val storeLogService: StoreLogService
) : UserStoreLogResource {

    override fun getInitLogs(
        userId: String,
        storeType: StoreTypeEnum,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return storeLogService.getInitLogs(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            tag = tag,
            executeCount = executeCount
        )
    }

    override fun getAfterLogs(
        userId: String,
        storeType: StoreTypeEnum,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return storeLogService.getAfterLogs(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            start = start,
            debug = debug,
            tag = tag,
            executeCount = executeCount
        )
    }

    override fun getMoreLogs(
        userId: String,
        storeType: StoreTypeEnum,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        return storeLogService.getMoreLogs(
            userId = userId,
            storeType = storeType,
            projectCode = projectCode,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            num = num,
            fromStart = fromStart,
            start = start,
            end = end,
            tag = tag,
            executeCount = executeCount
        )
    }
}
