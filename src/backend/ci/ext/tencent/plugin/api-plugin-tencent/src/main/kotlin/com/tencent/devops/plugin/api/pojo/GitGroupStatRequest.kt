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

package com.tencent.devops.plugin.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂项目组统计数据请求包")
data class GitGroupStatRequest(
    @ApiModelProperty("日期", required = true)
    val statDate: String,
    @ApiModelProperty("项目总数", required = true)
    val projectCount: Int,
    @ApiModelProperty("开源项目总数", required = true)
    val projectCountOpen: Int,
    @ApiModelProperty("项目增量", required = false)
    var projectIncre: Int,
    @ApiModelProperty("开源项目增量", required = false)
    var projectIncreOpen: Int,
    @ApiModelProperty("提交总数", required = true)
    val commitCount: Int,
    @ApiModelProperty("开源项目提交总数", required = true)
    val commitCountOpen: Int,
    @ApiModelProperty("提交增量", required = false)
    var commitIncre: Int,
    @ApiModelProperty("开源项目提交增量", required = false)
    var commitIncreOpen: Int,
    @ApiModelProperty("用户总数", required = true)
    val userCount: Int,
    @ApiModelProperty("开源项目用户总数", required = true)
    val userCountOpen: Int,
    @ApiModelProperty("提交用户增量", required = false)
    var userIncre: Int,
    @ApiModelProperty("开源项目提交用户增量", required = false)
    var userIncreOpen: Int
)
