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

package com.tencent.devops.metrics.service

import com.tencent.devops.metrics.pojo.`do`.AtomExecutionStatisticsInfoDO
import com.tencent.devops.metrics.pojo.`do`.ComplianceInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryAtomStatisticsInfoDTO
import com.tencent.devops.metrics.pojo.vo.AtomTrendInfoVO
import com.tencent.devops.metrics.pojo.vo.ListPageVO
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO

interface AtomStatisticsManageService {

    /**
     * 查询插件趋势信息
     * @param queryAtomTrendInfoDTO 插件统计信息查询传输对象
     * @return 插件趋势信息视图
     */
    fun queryAtomTrendInfo(
        queryAtomTrendInfoDTO: QueryAtomStatisticsInfoDTO
    ): AtomTrendInfoVO

    /**
     * 查询插件执行统计信息
     * @param queryAtomTrendInfoDTO 插件统计信息查询传输对象
     * @return 插件执行统计信息
     */
    fun queryAtomExecuteStatisticsInfo(
        queryAtomTrendInfoDTO: QueryAtomStatisticsInfoDTO
    ): ListPageVO<AtomExecutionStatisticsInfoDO>

    /**
     * 查询插件执行合规信息
     * @return 插件合规信息
     */
    fun queryAtomComplianceInfo(
        userId: String,
        atomCode: String,
        queryIntervalVO: QueryIntervalVO
    ): ComplianceInfoDO?
}
