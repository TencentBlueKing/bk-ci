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

package com.tencent.devops.scm

import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo

@Suppress("ALL")
interface IScm {
    val projectName: String
    val branchName: String?
    val url: String

    fun getLatestRevision(): RevisionInfo
    fun getBranches(
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<String>

    fun getTags(search: String? = null): List<String>

    // This is to check if the token & private key legal
    fun checkTokenAndPrivateKey()

    fun checkTokenAndUsername()

    fun addWebHook(hookUrl: String)

    fun createBranch(branch: String, ref: String) {}
    fun deleteBranch(branch: String) {}

    fun addCommitCheck(
        commitId: String,
        state: String,
        targetUrl: String,
        context: String,
        description: String,
        block: Boolean,
        targetBranch: List<String>?
    )

    fun addMRComment(mrId: Long, comment: String)

    fun lock(repoName: String, applicant: String, subpath: String)
    fun unlock(repoName: String, applicant: String, subpath: String)

    fun getCommits(branch: String?, all: Boolean, page: Int, size: Int): List<GitCommit> = emptyList()
    fun getCommitDiff(sha: String): List<GitDiff> = emptyList()

    fun getMergeRequestChangeInfo(mrId: Long): GitMrChangeInfo? = null

    fun getMrInfo(mrId: Long): GitMrInfo? = null

    fun getMrReviewInfo(mrId: Long): GitMrReviewInfo? = null

    fun getMrCommitList(mrId: Long, page: Int, size: Int) = emptyList<GitCommit>()

    fun getProjectInfo(projectName: String): GitProjectInfo? = null
}
