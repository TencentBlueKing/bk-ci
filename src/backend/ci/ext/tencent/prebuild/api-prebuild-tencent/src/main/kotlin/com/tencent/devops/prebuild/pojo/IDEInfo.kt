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

package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WebIDE实例状态")
data class IDEInfo(
    @ApiModelProperty("Web IDE实例状态")
    val ideInstanceStatus: Int,
    @ApiModelProperty("Agent实例状态")
    var agentInstanceStatus: Int,
    @ApiModelProperty("机器IP地址")
    val ip: String,
    @ApiModelProperty("ide实例的http服务url")
    val ideURL: String,
    @ApiModelProperty("web ide 版本")
    val ideVersion: String,
    @ApiModelProperty("机器类型")
    val serverType: String,
    @ApiModelProperty("服务器创建时间")
    val serverCreateTime: Long
)

@ApiModel("IDEAgent请求")
data class IDEAgentReq(
    @ApiModelProperty("项目名称", required = true)
    val projectId: String,
    @ApiModelProperty("devcloud服务器IP", required = true)
    val ip: String
)
