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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-指标配置修改信息")
data class IndicatorUpdate(
    @ApiModelProperty("原子的ClassType")
    val elementType: String? = null,
    @ApiModelProperty("原子名称")
    val elementName: String? = null,
    @ApiModelProperty("工具/原子子类")
    val elementDetail: String? = null,
    @ApiModelProperty("工具/原子版本")
    val elementVersion: String? = null,
    @ApiModelProperty("指标英文名")
    val enName: String? = null,
    @ApiModelProperty("指标中文名")
    val cnName: String? = null,
    @ApiModelProperty("指标所包含基础数据")
    val metadataIds: String? = null,
    @ApiModelProperty("默认操作类型")
    val defaultOperation: String? = null,
    @ApiModelProperty("可用操作")
    val operationAvailable: String? = null,
    @ApiModelProperty("默认阈值")
    val threshold: String? = null,
    @ApiModelProperty("阈值类型")
    val thresholdType: String? = null,
    @ApiModelProperty("描述")
    val desc: String? = null,
    @ApiModelProperty("是否可修改")
    val readOnly: Boolean? = null,
    @ApiModelProperty("阶段")
    val stage: String? = null,
    @ApiModelProperty("可见范围")
    val range: String? = null,
    @ApiModelProperty("指标标签，用于前端区分控制")
    val tag: String? = null,
    @ApiModelProperty("是否启用")
    val enable: Boolean? = null,
    @ApiModelProperty("指标类型")
    val type: IndicatorType? = IndicatorType.SYSTEM,
    @ApiModelProperty("输出日志详情")
    val logPrompt: String? = ""
)
