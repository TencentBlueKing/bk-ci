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
 *   "id": 187253,
 *   "url": "http://svn.xx.com/tsvn/rest/tgit/web/hooks/event/push",
 *   "created_at": "2018-03-13T13:46:08+0000",
 *   "project_id": 49170,
 *   "push_events": true,
 *   "issues_events": false,
 *   "merge_requests_events": false,
 *   "tag_push_events": false,
 *   "note_events": false,
 *   "review_events": false
 *   }
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHook(
    val id: Long,
    val url: String,
    @JsonProperty("project_id")
    val projectId: Long,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("push_events")
    val pushEvents: Boolean,
    @JsonProperty("tag_push_events")
    val tagPushEvents: Boolean,
    @JsonProperty("issues_events")
    val issuesEvents: Boolean,
    @JsonProperty("merge_requests_events")
    val mergeRequestsEvents: Boolean,
    @JsonProperty("note_events")
    val noteEvents: Boolean,
    @JsonProperty("review_events")
    val reviewEvents: Boolean
)
