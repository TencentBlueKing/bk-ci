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

package com.tencent.devops.stream.trigger.pojo.enums

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_CONCLUSION_FAILURE
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_CONCLUSION_SUCCESS
import com.tencent.devops.scm.code.git.api.GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS

enum class StreamCommitCheckState {
    PENDING,
    SUCCESS,
    ERROR,
    FAILURE
}

fun StreamCommitCheckState.toGitState(scmType: ScmType): String {
    return when (scmType) {
        ScmType.CODE_GIT -> when (this) {
            StreamCommitCheckState.PENDING -> "pending"
            StreamCommitCheckState.SUCCESS -> "success"
            StreamCommitCheckState.ERROR -> "error"
            StreamCommitCheckState.FAILURE -> "failure"
        }
        ScmType.GITHUB -> when (this) {
            StreamCommitCheckState.PENDING -> GITHUB_CHECK_RUNS_STATUS_IN_PROGRESS
            StreamCommitCheckState.SUCCESS -> GITHUB_CHECK_RUNS_CONCLUSION_SUCCESS
            StreamCommitCheckState.ERROR -> GITHUB_CHECK_RUNS_CONCLUSION_FAILURE
            StreamCommitCheckState.FAILURE -> GITHUB_CHECK_RUNS_CONCLUSION_FAILURE
        }
        else -> TODO("对接其他Git平台时需要补充")
    }
}
