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

package com.tencent.devops.quality.pojo.po

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "质量红线指标PO")
data class QualityIndicatorPO(
    @get:Schema(title = "ID")
    val id: Long,
    @get:Schema(title = "原子的ClassType")
    val elementType: String?,
    @get:Schema(title = "产出原子")
    var elementName: String?,
    @get:Schema(title = "工具/原子子类")
    val elementDetail: String?,
    @get:Schema(title = "指标英文名")
    val enName: String?,
    @get:Schema(title = "指标中文名")
    val cnName: String?,
    @get:Schema(title = "指标所包含基础数据")
    val metadataIds: String?,
    @get:Schema(title = "默认操作")
    val defaultOperation: String?,
    @get:Schema(title = "可用操作")
    val operationAvailable: String?,
    @get:Schema(title = "默认阈值")
    val threshold: String?,
    @get:Schema(title = "阈值类型")
    val thresholdType: String?,
    @get:Schema(title = "描述")
    var desc: String?,
    @get:Schema(title = "是否可修改")
    val indicatorReadOnly: Boolean?,
    @get:Schema(title = "阶段")
    val stage: String?,
    @get:Schema(title = "可见项目范围")
    val indicatorRange: String?,
    @get:Schema(title = "是否启用")
    val enable: Boolean?,
    @get:Schema(title = "指标类型")
    val type: String?,
    @get:Schema(title = "指标标签，用于前端区分控制")
    val tag: String?,
    @get:Schema(title = "创建用户")
    val createUser: String?,
    @get:Schema(title = "更新用户")
    val updateUser: String?,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime?,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime?,
    @get:Schema(title = "插件版本号")
    val atomVersion: String,
    @get:Schema(title = "用户自定义提示日志")
    val logPrompt: String,
    @get:Schema(title = "指标权重")
    val weight: Int? = null
)
