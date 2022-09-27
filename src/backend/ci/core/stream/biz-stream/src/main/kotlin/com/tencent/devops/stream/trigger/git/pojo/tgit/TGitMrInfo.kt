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

package com.tencent.devops.stream.trigger.git.pojo.tgit

import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitMrInfo

data class TGitMrInfo(
    override val mergeStatus: String,
    val baseCommit: String?,
    val baseInfo: GitMrInfo? = null
) : StreamGitMrInfo

enum class TGitMrStatus(val value: String) {
    MERGE_STATUS_UNCHECKED("unchecked"),
    MERGE_STATUS_CAN_BE_MERGED("can_be_merged"),
    MERGE_STATUS_CAN_NOT_BE_MERGED("cannot_be_merged")
    // 项目有配置 mr hook，当创建mr后，发送mr hook前，这个状态是hook_intercept,与stream无关
    // MERGE_STATUS_HOOK_INTERCEPT("hook_intercept")
}
