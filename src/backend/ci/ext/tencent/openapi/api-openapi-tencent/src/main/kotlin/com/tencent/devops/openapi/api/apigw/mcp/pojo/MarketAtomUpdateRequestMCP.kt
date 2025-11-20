/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.openapi.api.apigw.mcp.pojo

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件市场工作台-升级插件请求报文体")
data class MarketAtomUpdateRequestMCP(
    @get:Schema(title = "插件代码,在task.json文件中会定义", required = true)
    val atomCode: String,
    @get:Schema(
        title = "插件类型，填英文，仅在插件版本为1.0.0时填写。可选值：编译类插件(compileBuild)、测试类插件(test)、部署类插件(deploy)、其它插件(common)",
        required = false
    )
    val classifyCode: String?,
    @get:Schema(title = "适用Job类型，仅在插件版本为1.0.0时填写。AGENT： 编译环境，AGENT_LESS：无编译环境", required = false)
    val jobType: JobTypeEnum? = JobTypeEnum.AGENT,
    @get:Schema(title = "支持的操作系统，填数组，仅在插件版本为1.0.0时填写。可选值：LINUX、WINDOWS、MACOS", required = false)
    val os: ArrayList<String>? = arrayListOf(),
    @get:Schema(title = "插件简介,在task.json文件中会定义，仅在插件版本为1.0.0时填写。", required = false)
    @field:BkField(maxLength = 256)
    val summary: String?,
    @get:Schema(title = "插件描述,在task.json文件中会定义，仅在插件版本为1.0.0时填写。", required = false)
    val description: String?,
    @get:Schema(title = "版本日志内容", required = true)
    val versionContent: String = "",
    @get:Schema(title = "当前测试分支", required = true)
    val branch: String = ""
)
