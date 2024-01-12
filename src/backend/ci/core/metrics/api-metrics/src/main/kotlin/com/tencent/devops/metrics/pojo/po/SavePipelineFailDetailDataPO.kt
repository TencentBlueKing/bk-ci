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

package com.tencent.devops.metrics.pojo.po

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "保存流水线失败明细数据")
data class SavePipelineFailDetailDataPO(
    @Schema(name = "主键ID")
    val id: Long,
    @Schema(name = "项目ID")
    val projectId: String,
    @Schema(name = "流水线ID")
    val pipelineId: String,
    @Schema(name = "流水线名称")
    val pipelineName: String,
    @Schema(name = "渠道代码")
    val channelCode: String,
    @Schema(name = "构建ID")
    val buildId: String,
    @Schema(name = "构建序号")
    val buildNum: Int,
    @Schema(name = "触发代码库地址")
    val repoUrl: String? = null,
    @Schema(name = "触发代码库分支")
    val branch: String? = null,
    @Schema(name = "启动用户")
    val startUser: String,
    @Schema(name = "启动时间")
    val startTime: LocalDateTime? = null,
    @Schema(name = "结束时间")
    val endTime: LocalDateTime? = null,
    @Schema(name = "错误类型")
    val errorType: Int? = null,
    @Schema(name = "错误码")
    val errorCode: Int? = null,
    @Schema(name = "错误信息")
    val errorMsg: String? = null,
    @Schema(name = "统计时间")
    val statisticsTime: LocalDateTime,
    @Schema(name = "创建人")
    val creator: String,
    @Schema(name = "修改人")
    val modifier: String,
    @Schema(name = "创建时间")
    val createTime: LocalDateTime,
    @Schema(name = "更新时间")
    val updateTime: LocalDateTime
)
