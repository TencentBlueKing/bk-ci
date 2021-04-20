/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.npm.pojo.metadata

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * reference: https://docs.npmjs.com/cli/v6/configuring-npm/package-json
 */
data class NpmVersionMetadata(
    var deprecated: String? = null,
    val version: String,
    val name: String,
    @JsonProperty("publish-time")
    var publishTime: String? = null,
    var description: String? = null,
    // list or string
    var keywords: Any? = null,
    @JsonProperty("dist-tags")
    var distTags: Map<String, String>? = null,
    // 使用MutableList，服务器可能会添加当前操作用户到maintainer中
    var maintainers: MutableList<Map<String, String>>? = null,
    var author: Any? = null,
    var repository: Any? = null,
    var dist: NpmVersionDist,
    var dependencies: Map<String, Any>? = null,
    var readme: String? = null,
    var readmeFilename: String? = null,
    var homepage: String? = null,
    var bugs: Any? = null,
    var license: Any? = null
) : NpmMetadata()
