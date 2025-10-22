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

package com.tencent.devops.store.pojo.atom

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件参数替换信息")
data class AtomParamReplaceInfo(
    @get:Schema(title = "被替换插件参数名称", required = true)
    val fromParamName: String,
    @get:Schema(title = "替换插件参数名称", required = true)
    val toParamName: String,
    @get:Schema(title = "替换插件参数值，不传默认用被替换插件参数值替换", required = false)
    val toParamValue: Any? = null,
    @get:Schema(title = "替换插件默认参数值，如果没有指定替换插件参数值且被替换插件参数没有值则用该默认值作为替换插件参数值", required = false)
    val toParamDefaultValue: Any? = null,
    @get:Schema(title = "参数自定义转换接口url地址，接口参数结构需统一", required = false)
    val paramConvertUrl: String? = null
)
