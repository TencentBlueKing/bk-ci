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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
 *   "name": "v1.0.3",
 *   "message": "add tag v1.0.3",
 *   "commit": {
 *     "id": "dc74465c976b87c108a00b1b7e9b0f5f6d2c7ff8",
 *     "message": "update the yyyy report and vm dispatch logic\n",
 *     "parent_ids": [
 *       "6bcbc858d38661d9526682f15e37f0f9a6047635"
 *     ],
 *     "authored_date": "2018-04-20T10:37:44.000+08:00",
 *     "author_name": "yy",
 *     "author_email": "yy@xx.com",
 *     "committed_date": "2018-04-20T10:37:44.000+08:00",
 *     "committer_name": "yy",
 *     "committer_email": "yy@tencent.com"
 *   },
 * "release": null
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitTag(
    val name: String,
    val message: String?,
    val commit: GitTagCommit
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitTagCommit(
    val id: String,
    val message: String,
    @JsonProperty("authored_date")
    val authoredDate: String,
    @JsonProperty("author_name")
    val authorName: String,
    @JsonProperty("author_email")
    val authorEmail: String
)