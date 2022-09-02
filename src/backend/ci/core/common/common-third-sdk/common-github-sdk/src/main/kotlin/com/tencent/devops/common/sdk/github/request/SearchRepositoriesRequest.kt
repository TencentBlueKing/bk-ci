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
 *
 */

package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.SearchRepositoriesResponse

/**
 * 参考：https://docs.github.com/en/search-github/searching-on-github/searching-for-repositories
 */
data class SearchRepositoriesRequest(

    // Can be one of: stars, forks, help-wanted-issues, updated,Default: best match
    val sort: String? = null,
    // Can be one of: desc, asc,Default: desc
    val order: String? = null,
    @JsonProperty("per_page")
    var perPage: Int = 30,
    var page: Int = 1,
    val terms: MutableList<String> = mutableListOf()
) : GithubRequest<SearchRepositoriesResponse>() {

    val q: String
        get() = terms.joinToString(" ")

    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "search/repositories"

    /**
     * 按照仓库size搜索
     */
    fun size(v: String) {
        terms.add("size:$v")
    }

    /**
     * 按照组织搜索
     */
    fun org(v: String) {
        terms.add("org:$v")
    }

    /**
     * 按照用户搜索
     */
    fun user(v: String) {
        terms.add("user:$v")
    }

    /**
     * 按照具体仓库搜索
     */
    fun repo(v: String) {
        terms.add("repo:$v")
    }

    /**
     * 按照名称搜索
     */
    fun name(v: String) {
        terms.add("$v in:name")
    }
}
