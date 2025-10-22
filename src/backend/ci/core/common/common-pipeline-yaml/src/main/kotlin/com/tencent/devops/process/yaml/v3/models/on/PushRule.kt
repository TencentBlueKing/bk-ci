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

package com.tencent.devops.process.yaml.v3.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PushRule(
    val name: String? = null,
    val enable: Boolean? = true,
    val branches: List<String>?,

    @get:Schema(title = "branches-ignore")
    @JsonProperty("branches-ignore")
    val branchesIgnore: List<String>? = null,

    val paths: List<String>? = null,

    @get:Schema(title = "paths-ignore")
    @JsonProperty("paths-ignore")
    val pathsIgnore: List<String>? = null,

    val users: List<String>? = null,

    @get:Schema(title = "users-ignore")
    @JsonProperty("users-ignore")
    val usersIgnore: List<String>? = null,

    val action: List<String>? = null,

    @get:Schema(title = "path-filter-type")
    @JsonProperty("path-filter-type")
    val pathFilterType: String? = null,

    @get:Schema(title = "custom-filter")
    @JsonProperty("custom-filter")
    val custom: CustomFilter? = null
)

data class CustomFilter(
    @get:Schema(title = "custom-filter-url")
    @JsonProperty("url")
    val url: String? = null,

    @get:Schema(title = "custom-filter-credentials")
    @JsonProperty("credentials")
    val credentials: String? = null
)