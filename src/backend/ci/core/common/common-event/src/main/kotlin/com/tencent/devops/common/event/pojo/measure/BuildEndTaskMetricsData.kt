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

package com.tencent.devops.common.event.pojo.measure

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建结束后task指标数据")
data class BuildEndTaskMetricsData(
    @ApiModelProperty("taskId", required = true)
    val taskId: String,
    @ApiModelProperty("插件名称", required = true)
    val atomName: String,
    @ApiModelProperty("插件标识", required = true)
    val atomCode: String,
    @ApiModelProperty("插件在model中的位置", required = true)
    val atomPosition: String,
    @ApiModelProperty("插件分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("插件分类名称", required = true)
    val classifyName: String,
    @ApiModelProperty("执行开始时间", required = false)
    val startTime: String?,
    @ApiModelProperty("执行结束时间", required = false)
    val endTime: String?,
    @ApiModelProperty("task构建耗时", required = true)
    val costTime: Long,
    @ApiModelProperty("是否执行成功", required = true)
    val successFlag: Boolean,
    @ApiModelProperty("错误类型", required = false)
    val errorType: Int? = null,
    @ApiModelProperty("错误码", required = false)
    val errorCode: Int? = null,
    @ApiModelProperty("错误描述", required = false)
    val errorMsg: String? = null
)
