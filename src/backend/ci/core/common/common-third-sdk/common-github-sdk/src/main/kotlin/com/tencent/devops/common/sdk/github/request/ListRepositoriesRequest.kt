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

package com.tencent.devops.common.sdk.github.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.pojo.GithubRepo

@Suppress("all")
class ListRepositoriesRequest(
    // Limit results to repositories with the specified visibility.
    // Default: all
    // Can be one of: all, public, private
    val visibility: String? = null,
    // Comma-separated list of values. Can include:
    // * owner: Repositories that are owned by the authenticated user.
    // * collaborator: Repositories that the user has been added to as a collaborator.
    // * organization_member: Repositories that the user has access to through being a member of an organization.
    // This includes every repository on every team that the user is on.
    // Default: owner,collaborator,organization_member
    val affiliation: String? = null,
    // Limit results to repositories of the specified type.
    // Will cause a 422 error if used in the same request as visibility or affiliation.
    // Default: all
    // Can be one of: all, owner, public, private, member
    val type: String? = null,
    // The property to sort the results by.
    // Default: full_name
    // Can be one of: created, updated, pushed, full_name
    var sort: String? = null,
    // The order to sort by. Default: asc when using full_name, otherwise desc.
    // Can be one of: asc, desc
    var direction: String? = null,
    @JsonProperty("per_page")
    var perPage: Int = 30,
    var page: Int = 1,
    // Only show notifications updated after the given time. This is a timestamp in ISO 8601
    // format: YYYY-MM-DDTHH:MM:SSZ.
    val since: String? = null,
    // Only show notifications updated before the given time. This is a timestamp in ISO 8601
    // format: YYYY-MM-DDTHH:MM:SSZ.
    val before: String? = null
) : GithubRequest<List<GithubRepo>>() {
    override fun getHttpMethod(): HttpMethod {
        return HttpMethod.GET
    }

    override fun getApiPath(): String {
        return "/user/repos"
    }
}
