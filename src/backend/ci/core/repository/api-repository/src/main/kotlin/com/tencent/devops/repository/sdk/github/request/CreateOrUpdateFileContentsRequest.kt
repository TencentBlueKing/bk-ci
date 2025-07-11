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

package com.tencent.devops.repository.sdk.github.request

import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.common.json.JsonIgnorePath
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.pojo.GithubCommitUser
import com.tencent.devops.repository.sdk.github.response.CreateOrUpdateFileContentsResponse
import org.apache.commons.lang3.StringUtils

data class CreateOrUpdateFileContentsRequest(
    // id或owner/repo
    @JsonIgnorePath
    val repoName: String,
    @JsonIgnorePath
    val path: String,
    // The commit message
    val message: String,
    // The new file content, using Base64 encoding.
    val content: String,
    // Required if you are updating a file. The blob SHA of the file being replaced.
    val sha: String? = null,
    val branch: String? = null,
    // The person that committed the file. Default: the authenticated user.
    val committer: GithubCommitUser? = null,
    // The author of the file. Default: The committer or the authenticated user if you omit committer.
    val author: GithubCommitUser? = null
) : GithubRequest<CreateOrUpdateFileContentsResponse>() {
    override fun getHttpMethod(): HttpMethod {
        return HttpMethod.PUT
    }

    override fun getApiPath() = if (StringUtils.isNumeric(repoName)) {
        "repositories/$repoName/contents/$path"
    } else {
        "repos/$repoName/contents/$path"
    }
}
