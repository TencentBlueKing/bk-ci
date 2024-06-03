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

package com.tencent.devops.stream.trigger.git.pojo.github

import com.tencent.devops.stream.trigger.git.pojo.StreamGitMrInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrStatus

data class GithubMrInfo(
    override val mergeStatus: String,
    val baseCommit: String?
) : StreamGitMrInfo

enum class GitHubMrStatus(val value: String) {
    // Merge conflict. Merging will be blocked
    DIRTY("dirty"),
    // Mergeability was not checked yet. Merging will be blocked
    UNKNOWN("unknown"),
    // Blocked by a failing/missing required status check.
    BLOCKED("blocked"),
    // Head branch is behind the base branch. Only if required status checks is enabled but loose policy is not.
    // Merging will be blocked.
    BEHIND("behind"),
    // Failing/pending commit status that is not part of the required status checks. Merging is allowed (yellow box).
    UNSTABLE("unstable"),
    // GitHub Enterprise only, if a repo has custom pre-receive hooks. Merging is allowed (green box)
    HAS_HOOKS("has_hooks"),
    //  No conflicts, everything good. Merging is allowed (green box).
    CLEAN("clean");

    companion object {
        fun convertTGitMrStatus(value: String): TGitMrStatus {
            return when (value) {
                "dirty" -> TGitMrStatus.MERGE_STATUS_CAN_NOT_BE_MERGED
                "unknown" -> TGitMrStatus.MERGE_STATUS_UNCHECKED
                "blocked" -> TGitMrStatus.MERGE_STATUS_CAN_BE_MERGED
                "behind" -> TGitMrStatus.MERGE_STATUS_CAN_NOT_BE_MERGED
                "unstable" -> TGitMrStatus.MERGE_STATUS_CAN_BE_MERGED
                "has_hooks" -> TGitMrStatus.MERGE_STATUS_CAN_BE_MERGED
                "clean" -> TGitMrStatus.MERGE_STATUS_CAN_BE_MERGED
                else -> TGitMrStatus.MERGE_STATUS_CAN_NOT_BE_MERGED
            }
        }
    }
}
