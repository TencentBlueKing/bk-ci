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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTurboResourceV4
import com.tencent.devops.turbo.api.IServiceTurboController
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTurboResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwTurboResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTurboResourceV4Impl::class.java)
    }

    override fun getTurboPlanByProjectIdAndCreatedDate(
        projectId: String,
        startTime: String?,
        endTime: String?,
        pageNum: Int?,
        pageSize: Int?,
        userId: String
    ): Response<Page<TurboPlanStatRowVO>> {
        logger.info(
            "OPENAPI_TURBO_V4|$userId|get turbo plan by project id and created date|$projectId|$startTime" +
                "|$endTime|$pageNum|$pageSize"
        )
        return client.getSpringMvc(IServiceTurboController::class).getTurboPlanByProjectIdAndCreatedDate(
            projectId = projectId,
            startTime = DateTimeUtil.stringToLocalDate(startTime),
            endTime = DateTimeUtil.stringToLocalDate(endTime),
            pageNum = pageNum,
            pageSize = pageSize,
            userId = userId
        )
    }

    override fun getTurboRecordHistoryList(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?,
        turboRecordModel: TurboRecordModel,
        projectId: String,
        userId: String
    ): Response<Page<TurboRecordHistoryVO>> {
        logger.info(
            "OPENAPI_TURBO_V4|$userId|get turbo record history list|$projectId|$pageNum|$pageSize|$sortField" +
                "|$sortType|$turboRecordModel|$"
        )
        return client.getSpringMvc(IServiceTurboController::class).getTurboRecordHistoryList(
            pageNum = pageNum,
            pageSize = pageSize,
            sortField = sortField,
            sortType = sortType,
            turboRecordModel = turboRecordModel,
            projectId = projectId,
            userId = userId
        )
    }

    override fun getTurboPlanDetailByPlanId(
        planId: String,
        projectId: String,
        userId: String
    ): Response<TurboPlanDetailVO> {
        logger.info(
            "OPENAPI_TURBO_V4|$userId|get turbo plan detail by plan id|$projectId|$planId"
        )
        logger.info("getTurboPlanDetail: userId[$userId] projectId[$projectId] planId[$planId]")
        return client.getSpringMvc(IServiceTurboController::class).getTurboPlanDetailByPlanId(
            planId = planId,
            projectId = projectId,
            userId = userId
        )
    }
}
