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

package com.tencent.devops.process.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "")
data class TemplateInstancePage(
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "模板id", required = false)
    val templateId: String,
    @get:Schema(title = "模板生成的流水线实例列表", required = false)
    val instances: List<TemplatePipeline>,
    @get:Schema(title = "最新版本", required = false)
    val latestVersion: TemplateVersion,
    @get:Schema(title = "数量", required = false)
    val count: Int,
    @get:Schema(title = "页数", required = false)
    val page: Int?,
    @get:Schema(title = "每页数量", required = false)
    val pageSize: Int?,
    @get:Schema(title = "是否有创建模板实例权限", required = false)
    val hasCreateTemplateInstancePerm: Boolean? = null
)
