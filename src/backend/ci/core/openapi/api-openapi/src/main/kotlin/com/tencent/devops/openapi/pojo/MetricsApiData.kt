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
package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "api度量数据")
data class MetricsApiData(
    @get:Schema(title = "接口代码")
    val api: String,
    @get:Schema(title = "请求app code/ user id")
    val key: String,
    @get:Schema(title = "秒级并发量")
    var secondLevelConcurrency: Int? = null,
    @get:Schema(title = "峰值并发量")
    var peakConcurrency: Int? = null,
    @get:Schema(title = "5min调用量")
    var call5m: Int? = null,
    @get:Schema(title = "1h调用量")
    var call1h: Int? = null,
    @get:Schema(title = "24h调用量")
    var call24h: Int? = null,
    @get:Schema(title = "7d调用量")
    var call7d: Int? = null
)
