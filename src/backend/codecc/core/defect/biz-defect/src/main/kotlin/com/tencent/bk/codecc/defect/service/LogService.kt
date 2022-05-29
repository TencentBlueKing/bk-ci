/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.task.vo.QueryLogRepVO


/**
 * 日志服务接口
 *
 * @date 2019/7/11
 */
interface LogService {


    /**
     * 查询分析记录日志
     *
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建号ID
     * @param queryKeywords 搜索词
     * @param tag 对应element ID
     * @return 日志信息
     */
    fun getAnalysisLog(userId: String, projectId: String, pipelineId: String, buildId: String,
                       queryKeywords: String?, tag: String?): QueryLogRepVO?


    /**
     * 查询更多的日志
     *
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建号ID
     * @param num 行数
     * @param fromStart 是否顺序显示
     * @param start 开始行数
     * @param end 结束行数
     * @param tag 对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    fun getMoreLogs(userId: String, projectId: String, pipelineId: String, buildId: String, num: Int?,
                    fromStart: Boolean?, start: Long, end: Long, tag: String?, executeCount: Int?): QueryLogRepVO


    /**
     * 下载分析记录日志
     *
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建号ID
     * @param tag 对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    fun downloadLogs(userId: String, projectId: String, pipelineId: String, buildId: String,
                     tag: String?, executeCount: Int?)


    /**
     * 获取某行后的日志
     *
     * @param projectId    项目ID
     * @param pipelineId   流水线ID
     * @param buildId      构建号ID
     * @param start        开始行数
     * @param queryKeywords 搜索词
     * @param tag          对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    fun getAfterLogs(userId: String, projectId: String, pipelineId: String, buildId: String,
                     start: Long, queryKeywords: String?, tag: String?, executeCount: Int?): QueryLogRepVO
}