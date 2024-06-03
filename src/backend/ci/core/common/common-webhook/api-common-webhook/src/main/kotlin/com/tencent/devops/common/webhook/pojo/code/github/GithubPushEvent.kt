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

package com.tencent.devops.common.webhook.pojo.code.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPushEvent(
    @JsonProperty("after")
    val after: String, // 5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
    @JsonProperty("base_ref")
    val baseRef: String?, // null
    @JsonProperty("before")
    val before: String, // 04c82898ae5093e0b9b7b1aa7a520bc390063c97
    @JsonProperty("commits")
    val commits: List<GithubCommit>,
    @JsonProperty("compare")
    val compare: String, // https://github.com/yongyiduan/webhook-test/compare/04c82898ae50...5f76b8875980
    @JsonProperty("created")
    val created: Boolean, // false
    @JsonProperty("deleted")
    val deleted: Boolean, // false
    @JsonProperty("forced")
    val forced: Boolean, // false
    @JsonProperty("head_commit")
    val headCommit: GithubHeadCommit?,
    @JsonProperty("pusher")
    val pusher: GithubPusher,
    @JsonProperty("ref")
    val ref: String, // refs/heads/main
    @JsonProperty("repository")
    val repository: GithubRepository,
    @JsonProperty("sender")
    override val sender: GithubUser
) : GithubEvent(sender) {
    companion object {
        const val classType = "push"
    }
}

/*
* 兼容 tGit
*/
fun GithubPushEvent.checkCreateAndUpdate(): Boolean? = when {
    this.created && this.commits.isEmpty() -> false
    this.created && this.commits.isNotEmpty() -> true
    else -> null
}
