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

package com.tencent.devops.plugin.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "金刚扫面任务")
data class JinGangApp(
    @get:Schema(title = "任务Id")
    val id: Long,
    @get:Schema(title = "项目Id")
    val projectId: String,
    @get:Schema(title = "流水线Id")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "构建Id")
    val buildId: String,
    @get:Schema(title = "构建号")
    val buildNo: Int,
    @get:Schema(title = "版本号")
    val version: String,
    @get:Schema(title = "包名称")
    val fileName: String,
    @get:Schema(title = "文件MD5")
    val fileMD5: String,
    @get:Schema(title = "文件大小(Byte)")
    val fileSize: Long,
    @get:Schema(title = "开始时间")
    val createTime: Long,
    @get:Schema(title = "更新时间")
    val updateTime: Long,
    @get:Schema(title = "执行人")
    val creator: String,
    @get:Schema(title = "状态(成功;失败;扫描中)")
    val status: String,
    @get:Schema(title = "类型(android;ios;其他)")
    val type: String

)
