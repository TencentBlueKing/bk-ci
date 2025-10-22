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

package com.tencent.devops.scm.code.svn.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * {
 *   "status":"200",
 *   "message":"查询成功",
 *   "exist":true,
 *   "webhooks":[
 *     {
 *       "id":802,
 *       "userName":"xx",
 *       "repName":"pp/hello_world_proj",
 *       "path":"hello_world_proj",
 *       "eventType":1,
 *       "callBack":"http://xx.com/ms/repository/api/codesvn/commit",
 *       "confTime":1526612489000
 *     }
 *   ]
 * }
 *
 *
 * {"status":"200","message":"查询成功","exist":false,"webhooks":[]}
 *
 *
 * {
 *   "status":"200",
 *   "message":"查询成功",
 *   "exist":true,
 *   "webhooks":[
 *     {
 *       "id":802,
 *       "userName":"xx",
 *       "repName":"proj/hello_world_proj",
 *       "path":"hello_world_proj",
 *       "eventType":1,
 *       "callBack":"http://xx.com/ms/repository/api/codesvn/commit",
 *       "confTime":1526612489000
 *     },
 *     {
 *       "id":891,
 *       "userName":"xx",
 *       "repName":"proj/hello_world_proj",
 *       "path":"hello_world_proj/trunk",
 *       "eventType":1,
 *       "callBack":"http://xx.com/ms/repository/api/codesvn/commit",
 *       "confTime":1526699650000
 *     }
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SVNWebHook(
    val status: String,
    val message: String,
    val exist: Boolean,
    val webhooks: List<Hook>
)

data class Hook(
    val id: Int,
    val userName: String,
    val repName: String,
    val path: String,
    val eventType: Int,
    val callBack: String,
    val confTime: Long
)

data class SvnHook(
    val id: Int,
    val url: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("project_id")
    val projectId: String,
    @JsonProperty("svn_post_commit_events")
    val svnPostCommitEvents: Boolean,
    @JsonProperty("svn_pre_commit_events")
    val svnPreCommitEvents: Boolean,
    @JsonProperty("lock_events")
    val lockEvents: Boolean,
    val path: String
)

enum class SvnHookEventType(val value: String) {
    // 提交完成后，触发回调钩子
    SVN_POST_COMMIT_EVENTS("svn_post_commit_events"),

    // 提交时，这个钩子将被触发
    SVN_PRE_COMMIT_EVENTS("svn_pre_commit_events"),

    // 锁定文件或解锁时
    LOCK_EVENTS("lock_events");
}
