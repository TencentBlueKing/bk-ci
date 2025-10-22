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

package com.tencent.devops.image.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "检查镜像信息返回模型")
data class CheckDockerImageResponse(
    @get:Schema(title = "错误代码")
    val errorCode: Int,
    @get:Schema(title = "错误信息")
    val errorMessage: String? = "",
    @get:Schema(title = "架构")
    val arch: String? = "",
    @get:Schema(title = "作者")
    val author: String? = "",
    @get:Schema(title = "评论")
    val comment: String? = "",
    @get:Schema(title = "创建时间")
    val created: String? = "",
    @get:Schema(title = "docker版本")
    val dockerVersion: String? = "",
    @get:Schema(title = "id")
    val id: String? = "",
    @get:Schema(title = "操作系统")
    val os: String? = "",
    @get:Schema(title = "操作系统版本")
    val osVersion: String? = "",
    @get:Schema(title = "父容器")
    val parent: String? = "",
    @get:Schema(title = "大小")
    val size: Long? = 0,
    @get:Schema(title = "仓库标签")
    val repoTags: List<String>? = null,
    @get:Schema(title = "image存储属性")
    val repoDigests: List<String>? = null,
    @get:Schema(title = "虚拟大小")
    val virtualSize: Long? = 0
)
