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

package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(name = "质量红线-指标配置修改信息")
data class IndicatorUpdate(
    @Schema(name = "原子的ClassType")
    val elementType: String? = null,
    @Schema(name = "原子名称")
    val elementName: String? = null,
    @Schema(name = "工具/原子子类")
    val elementDetail: String? = null,
    @Schema(name = "工具/原子版本")
    val elementVersion: String? = null,
    @Schema(name = "指标英文名")
    val enName: String? = null,
    @Schema(name = "指标中文名")
    val cnName: String? = null,
    @Schema(name = "指标所包含基础数据")
    val metadataIds: String? = null,
    @Schema(name = "默认操作类型")
    val defaultOperation: String? = null,
    @Schema(name = "可用操作")
    val operationAvailable: String? = null,
    @Schema(name = "默认阈值")
    val threshold: String? = null,
    @Schema(name = "阈值类型")
    val thresholdType: String? = null,
    @Schema(name = "描述")
    val desc: String? = null,
    @Schema(name = "是否可修改")
    val readOnly: Boolean? = null,
    @Schema(name = "阶段")
    val stage: String? = null,
    @Schema(name = "可见范围")
    val range: String? = null,
    @Schema(name = "指标标签，用于前端区分控制")
    val tag: String? = null,
    @Schema(name = "是否启用")
    val enable: Boolean? = null,
    @Schema(name = "指标类型")
    val type: IndicatorType? = IndicatorType.SYSTEM,
    @Schema(name = "输出日志详情")
    val logPrompt: String? = ""
)
