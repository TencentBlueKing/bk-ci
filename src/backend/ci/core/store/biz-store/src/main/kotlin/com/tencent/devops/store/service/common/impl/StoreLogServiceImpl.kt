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
package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreLogService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 研发商店-日志业务逻辑类
 * since: 2019-08-15
 */
@Suppress("ALL")
@Service
class StoreLogServiceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeMemberDao: StoreMemberDao
) : StoreLogService {

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
        val validateResult = validateUserQueryPermission(storeType, pipelineId, userId)
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getInitLogs(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug,
                tag = tag,
                jobId = null,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
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
        val validateResult = validateUserQueryPermission(storeType, pipelineId, userId)
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getAfterLogs(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                start = start,
                debug = debug,
                tag = tag,
                jobId = null,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
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
        val validateResult = validateUserQueryPermission(storeType, pipelineId, userId)
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getMoreLogs(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                debug = debug,
                num = num,
                fromStart = fromStart,
                start = start,
                end = end,
                tag = tag,
                jobId = null,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
    }

    private fun validateUserQueryPermission(
        storeType: StoreTypeEnum,
        pipelineId: String,
        userId: String
    ): Result<Boolean> {
        // 查询是否是插件的成员，只有插件的成员才能看日志
        val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRelByPipelineId(dslContext, pipelineId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(pipelineId),
                language = I18nUtil.getLanguage(userId)
            )
        val flag = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = storePipelineRelRecord.storeCode,
            storeType = storeType.type.toByte()
        )
        if (!flag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storePipelineRelRecord.storeCode)
            )
        }
        return Result(true)
    }
}
