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

package com.tencent.devops.quality.pojo.po

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("质量红线指标PO")
data class QualityIndicatorPO(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("产出原子")
    var elementName: String?,
    @ApiModelProperty("工具/原子子类")
    val elementDetail: String?,
    @ApiModelProperty("指标英文名")
    val enName: String?,
    @ApiModelProperty("指标中文名")
    val cnName: String?,
    @ApiModelProperty("指标所包含基础数据")
    val metadataIds: String?,
    @ApiModelProperty("默认操作")
    val defaultOperation: String?,
    @ApiModelProperty("可用操作")
    val operationAvailable: String?,
    @ApiModelProperty("默认阈值")
    val threshold: String?,
    @ApiModelProperty("阈值类型")
    val thresholdType: String?,
    @ApiModelProperty("描述")
    var desc: String?,
    @ApiModelProperty("是否可修改")
    val indicatorReadOnly: Boolean?,
    @ApiModelProperty("阶段")
    val stage: String?,
    @ApiModelProperty("可见项目范围")
    val indicatorRange: String?,
    @ApiModelProperty("是否启用")
    val enable: Boolean?,
    @ApiModelProperty("指标类型")
    val type: String?,
    @ApiModelProperty("指标标签，用于前端区分控制")
    val tag: String?,
    @ApiModelProperty("创建用户")
    val createUser: String?,
    @ApiModelProperty("更新用户")
    val updateUser: String?,
    @ApiModelProperty("创建时间")
    val createTime: LocalDateTime?,
    @ApiModelProperty("更新时间")
    val updateTime: LocalDateTime?,
    @ApiModelProperty("插件版本号")
    val atomVersion: String,
    @ApiModelProperty("用户自定义提示日志")
    val logPrompt: String,
    @ApiModelProperty("指标权重")
    val weight: Int? = null
)
