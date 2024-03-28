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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "WebIDE实例状态")
data class IDEInfo(
    @get:Schema(title = "Web IDE实例状态")
    val ideInstanceStatus: Int,
    @get:Schema(title = "Agent实例状态")
    var agentInstanceStatus: Int,
    @get:Schema(title = "机器IP地址")
    val ip: String,
    @get:Schema(title = "ide实例的http服务url")
    val ideURL: String,
    @get:Schema(title = "web ide 版本")
    val ideVersion: String,
    @get:Schema(title = "机器类型")
    val serverType: String,
    @get:Schema(title = "服务器创建时间")
    val serverCreateTime: Long
)

@Schema(title = "IDEAgent请求")
data class IDEAgentReq(
    @get:Schema(title = "项目名称", required = true)
    val projectId: String,
    @get:Schema(title = "devcloud服务器IP", required = true)
    val ip: String
)
