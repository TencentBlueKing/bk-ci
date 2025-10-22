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

package com.tencent.devops.store.atom.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomPipelineExecInfo
import java.time.LocalDateTime

interface MarketAtomStatisticService {

    /**
     * 根据插件标识获取插件关联的所有流水线列表（包括其他项目下）
     */
    fun getAtomPipelinesByCode(
        atomCode: String,
        username: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipeline>>

    /**
     * 根据插件标识获取插件关联的流水线信息（当前项目下）
     */
    fun getAtomPipelinesByProject(
        userId: String,
        projectCode: String,
        atomCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipelineExecInfo>>

    /**
     * 同步使用插件流水线数量到汇总数据统计表
     */
    fun asyncUpdateStorePipelineNum(): Boolean

    /**
     * 同步组件每日统计信息
     * @param storeType 组件类型
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     */
    fun asyncAtomDailyStatisticInfo(
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Boolean
}
