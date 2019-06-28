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

package com.tencent.devops.worker.common.api.report

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.worker.common.api.WorkerRestApiSDK
import java.io.File

interface ReportSDKApi : WorkerRestApiSDK {

    /**
     * 获取报告跟路径
     * @param taskId 创建这个报告的任务插件id
     * @return  链接地址
     */
    fun getRootUrl(taskId: String): Result<String>

    /**
     * 创建报告要上传的记录
     */
    fun createReportRecord(
        taskId: String,
        indexFile: String,
        name: String,
        reportType: String? = ReportTypeEnum.INTERNAL.name
    ): Result<Boolean>

    /**
     * 归档报告
     * @param file  报告首页文件
     * @param taskId 当前插件任务id
     * @param relativePath  报告首页所在的本地文件相对路径
     * @param buildVariables 构建变量
     */
    fun uploadReport(file: File, taskId: String, relativePath: String, buildVariables: BuildVariables)
}