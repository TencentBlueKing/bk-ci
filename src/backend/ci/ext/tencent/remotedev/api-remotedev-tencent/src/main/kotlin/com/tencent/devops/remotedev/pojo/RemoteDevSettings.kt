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

package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("远程开发配置")
data class RemoteDevSettings(
    @ApiModelProperty("默认shell")
    val defaultShell: String = "shell",
    @ApiModelProperty("客户端使用，后台只管存的信息")
    val basicSetting: Map<String, String> = emptyMap(),
    @ApiModelProperty("是否连接工蜂")
    val gitAttached: Boolean = false,
    @ApiModelProperty("是否连接TAPD")
    val tapdAttached: Boolean = false,
    @ApiModelProperty("是否连接GitHub")
    val githubAttached: Boolean = false,
    @ApiModelProperty("远程开发环境变量配置")
    val envsForVariable: Map<String, String> = emptyMap(),
    @ApiModelProperty("远程开发文件配置")
    val envsForFile: List<RemoteDevFile> = emptyList(),
    @ApiModelProperty("dotfiles仓库路径")
    val dotfileRepo: String = ""
)
