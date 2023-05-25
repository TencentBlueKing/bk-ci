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

package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("store组件对接平台信息")
data class StoreDockingPlatformInfo(
    @ApiModelProperty("环境变量ID", required = true)
    val id: String,
    @ApiModelProperty("平台代码", required = true)
    val platformCode: String,
    @ApiModelProperty("平台名称", required = true)
    val platformName: String,
    @ApiModelProperty("网址", required = false)
    val website: String?,
    @ApiModelProperty("简介", required = true)
    val summary: String,
    @ApiModelProperty("负责人", required = true)
    val principal: String,
    @ApiModelProperty("平台logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("标签", required = false)
    val labels: List<String>? = null,
    @ApiModelProperty("所属机构名称", required = true)
    val ownerDeptName: String,
    @ApiModelProperty("添加用户", required = true)
    val creator: String,
    @ApiModelProperty("修改用户", required = true)
    val modifier: String,
    @ApiModelProperty("添加时间", required = true)
    val createTime: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: String
)
