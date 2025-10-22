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
import org.jooq.Record
import org.jooq.Result

@Schema(title = "模板-权限实体")
data class TemplateWithPermission(
    @get:Schema(title = "拥有列表权限的模板记录", required = true)
    val templatesWithListPermRecords: Result<out Record>?,
    @get:Schema(title = "拥有查看权限的模板列表ID", required = true)
    val templatesWithViewPermIds: List<String>?,
    @get:Schema(title = "拥有编辑权限的模板列表ID", required = true)
    val templatesWithEditPermIds: List<String>?,
    @get:Schema(title = "拥有删除权限的模板列表ID", required = true)
    val templatesWithDeletePermIds: List<String>?,
    @get:Schema(title = "数量", required = true)
    val count: Int
)
