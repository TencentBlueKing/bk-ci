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

@Schema(title = "保存插件失败明细数据")
data class SaveAtomFailDetailDataPO(
    @get:Schema(title = "主键ID")
    val id: Long,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "渠道代码")
    val channelCode: String,
    @get:Schema(title = "构建ID")
    val buildId: String,
    @get:Schema(title = "构建序号")
    val buildNum: Int,
    @get:Schema(title = "插件代码")
    val atomCode: String,
    @get:Schema(title = "插件名称")
    val atomName: String,
    @get:Schema(title = "插件在model中的位置")
    val atomPosition: String,
    @get:Schema(title = "插件分类代码")
    val classifyCode: String,
    @get:Schema(title = "插件分类名称")
    val classifyName: String,
    @get:Schema(title = "启动用户")
    val startUser: String,
    @get:Schema(title = "启动时间")
    val startTime: LocalDateTime? = null,
    @get:Schema(title = "结束时间")
    val endTime: LocalDateTime? = null,
    @get:Schema(title = "错误类型")
    val errorType: Int? = null,
    @get:Schema(title = "错误码")
    val errorCode: Int? = null,
    @get:Schema(title = "错误信息")
    val errorMsg: String? = null,
    @get:Schema(title = "统计时间")
    val statisticsTime: LocalDateTime,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime
)
