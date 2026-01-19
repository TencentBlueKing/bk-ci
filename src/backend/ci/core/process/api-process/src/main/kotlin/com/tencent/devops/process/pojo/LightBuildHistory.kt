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
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 轻量构建历史 DTO，仅保留基础字段，减少序列化和传输开销
 */
@Schema(title = "轻量历史构建模型")
data class LightBuildHistory(
    @get:Schema(title = "构建ID", required = true)
    val id: String,
    @get:Schema(title = "启动用户", required = true)
    val userId: String,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int?,
    @get:Schema(title = "流水线的执行开始时间(yyyy-MM-dd HH:mm:ss格式)", required = true)
    val startTime: String,
    @get:Schema(title = "流水线的执行结束时间(yyyy-MM-dd HH:mm:ss格式)", required = false)
    val endTime: String?,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "备注", required = false)
    val remark: String?,
    @get:Schema(title = "运行耗时(毫秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @get:Schema(title = "是否重试", required = false)
    val retry: Boolean = false,
    @get:Schema(title = "触发条件", required = true)
    val trigger: String,
    @get:Schema(title = "流水线任务执行错误", required = false)
    val errorInfoList: List<ErrorInfo>?,
    @get:Schema(title = "启动参数", required = false)
    val buildParameters: List<LightBuildParameter>?
)

/**
 * 轻量构建参数
 */
@Schema(title = "轻量构建模型-构建参数")
data class LightBuildParameter(
    @get:Schema(title = "元素值ID-标识符", required = true)
    val key: String,
    @get:Schema(title = "元素值名称-显示用", required = true)
    val value: Any,
    @get:Schema(title = "元素值类型", required = false)
    val valueType: BuildFormPropertyType? = null,
    @get:Schema(title = "描述", required = false)
    val desc: String? = null,
    @get:Schema(title = "默认值", required = false)
    val defaultValue: Any? = null,
)