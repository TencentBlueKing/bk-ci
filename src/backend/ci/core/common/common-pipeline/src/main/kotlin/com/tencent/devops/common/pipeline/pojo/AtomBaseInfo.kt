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

package com.tencent.devops.common.pipeline.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件基本信息")
data class AtomBaseInfo(
    @ApiModelProperty("插件Id", required = true)
    val atomId: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件版本号", required = true)
    val version: String,
    @ApiModelProperty("插件状态", required = true)
    val atomStatus: String,
    @ApiModelProperty("插件开发语言", required = false)
    val language: String? = null,
    @ApiModelProperty("分支", required = false)
    val branch: String? = null,
    @ApiModelProperty("代码提交ID", required = false)
    val commitId: String? = null,
    @ApiModelProperty("支持的操作系统名称", required = false)
    val osName: String? = null,
    @ApiModelProperty("支持的操作系统架构", required = false)
    val osArch: String? = null,
    @ApiModelProperty("不支持的操作系统组合信息", required = false)
    val invalidOsInfo: String? = null,
    @ApiModelProperty("运行时版本", required = false)
    val runtimeVersion: String? = null
)
