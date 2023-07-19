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

@ApiModel("质量红线控制点PO")
data class ControlPointPO(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("原子的ClassType")
    val elementType: String,
    @ApiModelProperty("控制点名称(原子名称)")
    var name: String,
    @ApiModelProperty("研发阶段")
    var stage: String,
    @ApiModelProperty("支持红线位置(准入-BEFORE, 准出-AFTER)")
    val availablePosition: String,
    @ApiModelProperty("默认红线位置")
    val defaultPosition: String,
    @ApiModelProperty("是否启用")
    val enable: Boolean,
    @ApiModelProperty("创建用户")
    val createUser: String,
    @ApiModelProperty("更新用户")
    val updateUser: String,
    @ApiModelProperty("创建时间")
    val createTime: LocalDateTime,
    @ApiModelProperty("更新时间")
    val updateTime: LocalDateTime,
    @ApiModelProperty("更新时间")
    val atomVersion: String = "1.0.0",
    @ApiModelProperty("更新时间")
    val testProject: String = "",
    @ApiModelProperty("更新时间")
    val tag: String? = null,
    @ApiModelProperty("更新时间")
    val controlPointHashId: String? = null
)
