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

package com.tencent.devops.auth.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.auth.api.pojo.EsbBaseReq
import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class SearchUserAndDeptEntity(
    @get:Schema(title = "查找字段, 默认值为 'id'", description = "lookup_field")
    @JsonProperty("lookup_field")
    val lookupField: String? = null,
    @get:Schema(title = "返回值字段")
    val fields: String? = null,
    @get:Schema(title = "精确查找内容列表", description = "exact_lookups")
    @JsonProperty("exact_lookups")
    var exactLookups: Any? = null,
    @get:Schema(title = "模糊查找内容列表", description = "fuzzy_lookups")
    @JsonProperty("fuzzy_lookups")
    var fuzzyLookups: Any? = null,
    @get:Schema(title = "用户登录态信息", description = "access_token")
    @JsonProperty("access_token")
    val accessToken: String? = null,
    @get:Schema(title = "第几页", description = "page_size")
    val page: Int? = null,
    @get:Schema(title = "分页大小", description = "page_size")
    @JsonProperty("page_size")
    val pageSize: Int? = 200,
    override var bk_app_code: String,
    override var bk_app_secret: String,
    override var bk_username: String = "",
    override val bk_token: String = ""
) : EsbBaseReq(bk_app_code, bk_app_secret, bk_username, bk_token)
