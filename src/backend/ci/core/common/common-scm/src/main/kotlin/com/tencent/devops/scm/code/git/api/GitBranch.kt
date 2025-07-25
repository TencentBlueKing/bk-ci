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

package com.tencent.devops.scm.code.git.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * {
 *     "name": "feature_kotlin",
 *     "protected": false,
 *     "developers_can_push": false,
 *     "developers_can_merge": false,
 *     "commit": {
 *         "id": "1465e3f05d02d4f831efc8702abcd8a45ae9",
 *         "message": "message is null\n",
 *         "parent_ids": [
 *             "a080a118eb15bfcb703ed1c86d8702abcd4588e"
 *         ],
 *         "authored_date": "2017-06-17T07:57:55+0000",
 *         "author_name": "xx",
 *         "author_email": "xx@cc.com",
 *         "committed_date": "2017-06-17T07:57:55+0000",
 *         "committer_name": "xx",
 *         "committer_email": "xx@cc.com",
 *         "title": "8702abcd",
 *         "short_id": "1465e3f0",
 *         "created_at": "2017-06-17T07:57:55+0000"
 *     }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitBranch(
    val name: String,
    val commit: GitBranchCommit
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitBranchCommit(
    val id: String,
    val message: String,
    @JsonProperty("authored_date")
    val authoredDate: String,
    @JsonProperty("author_name")
    val authorName: String,
    @JsonProperty("author_email")
    val authorEmail: String,
    val title: String
)
