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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult

@Suppress("ALL")
interface IScmService {
    fun getLatestRevision(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): RevisionInfo

    fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<String>

    fun deleteBranch(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    )

    fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String,
        search: String? = null
    ): List<String>

    fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): TokenCheckResult

    fun checkUsernameAndPassword(
        projectName: String,
        url: String,
        type: ScmType,
        username: String,
        password: String,
        token: String,
        region: CodeSvnRegion?,
        repoUsername: String
    ): TokenCheckResult

    fun addWebHook(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String? = null,
        hookUrl: String? = null
    )

    fun addCommitCheck(
        request: CommitCheckRequest
    )

    fun lock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    )

    fun unlock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    )

    fun createBranch(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String,
        ref: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    )

    fun listCommits(
        projectName: String,
        url: String,
        type: ScmType,
        branch: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        all: Boolean,
        page: Int,
        size: Int
    ): List<GitCommit>

    fun getCommitDiff(
        projectName: String,
        url: String,
        type: ScmType,
        sha: String,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): List<GitDiff>

    fun getMergeRequestChangeInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrChangeInfo?

    fun getMrInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrInfo?

    fun getMrReviewInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long
    ): GitMrReviewInfo?

    fun getMrCommitList(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?,
        mrId: Long,
        page: Int,
        size: Int
    ): List<GitCommit>

    /**
     * 获取代码库详情
     */
    fun getProjectInfo(
        projectName: String,
        url: String,
        type: ScmType,
        token: String?
    ): GitProjectInfo?
}
