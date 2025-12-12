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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.pojo.ErrorInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 轻量构建历史 DTO，仅保留基础字段，减少序列化和传输开销
 */
@Schema(title = "轻量历史构建模型")
data class LightBuildHistory(
    @get:Schema(title = "构建ID")
    val id: String,
    @get:Schema(title = "启动用户")
    val userId: String,
    @get:Schema(title = "构建号")
    val buildNum: Int?,
    @get:Schema(title = "流水线的执行开始时间(毫秒时间戳)")
    val startTime: Long,
    @get:Schema(title = "流水线的执行结束时间(毫秒时间戳)")
    val endTime: Long?,
    @get:Schema(title = "状态")
    val status: String,
    @get:Schema(title = "备注")
    val remark: String?,
    @get:Schema(title = "运行耗时(毫秒，不包括人工审核时间)")
    val executeTime: Long?,
    @get:Schema(title = "是否重试")
    val retry: Boolean,
    @get:Schema(title = "触发方式")
    val trigger: String?,
    @get:Schema(title = "流水线任务执行错误")
    val errorInfoList: List<ErrorInfo>?,
    @get:Schema(title = "阶段 & 任务错误信息")
    val stageStatus: List<LightStageStatus>?,
    @get:Schema(title = "构建参数")
    val buildParameters: List<LightBuildParameter>?
)

/**
 * 轻量阶段状态
 * 注意：这里只保留与前端展示/排障相关的必要字段
 */
@Schema(title = "轻量阶段状态")
data class LightStageStatus(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "阶段名称")
    val name: String?,
    @get:Schema(title = "阶段状态")
    val status: String?,
    @get:Schema(title = "提示信息")
    val showMsg: String?
)

/**
 * 轻量构建参数
 */
@Schema(title = "轻量构建参数")
data class LightBuildParameter(
    @get:Schema(title = "参数key")
    val key: String,
    @get:Schema(title = "参数值")
    val value: String?,
    @get:Schema(title = "值类型")
    val valueType: String?,
    @get:Schema(title = "描述")
    val desc: String?,
    @get:Schema(title = "默认值")
    val defaultValue: String?
)