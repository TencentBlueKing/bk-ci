/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.pojo

import com.tencent.bkrepo.common.query.model.Rule
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扫描任务信息")
data class ScanTask(
    @ApiModelProperty("任务id")
    val taskId: String,
    @ApiModelProperty("触发者")
    val createdBy: String,
    @ApiModelProperty("最后修改时间")
    val lastModifiedDateTime: String,
    @ApiModelProperty("任务触发时间")
    val triggerDateTime: String,
    @ApiModelProperty("任务开始执行时间")
    val startDateTime: String?,
    @ApiModelProperty("任务执行结束时间")
    val finishedDateTime: String?,
    @ApiModelProperty("任务状态")
    val status: String,
    @ApiModelProperty("扫描方案")
    val scanPlan: ScanPlan?,
    @ApiModelProperty("扫描文件匹配规则")
    val rule: Rule?,
    @ApiModelProperty("计划扫描文件总数")
    val total: Long,
    @ApiModelProperty("扫描中的文件总数")
    val scanning: Long,
    @ApiModelProperty("扫描失败的文件总数")
    val failed: Long,
    @ApiModelProperty("已扫描文件总数")
    val scanned: Long,
    @ApiModelProperty("使用的扫描器")
    val scanner: String,
    @ApiModelProperty("扫描器类型")
    val scannerType: String,
    @ApiModelProperty("扫描器版本")
    val scannerVersion: String,
    @ApiModelProperty("扫描结果统计数据")
    val scanResultOverview: Map<String, Long>?,
    @ApiModelProperty("是否强制扫描")
    val force: Boolean = false
)
