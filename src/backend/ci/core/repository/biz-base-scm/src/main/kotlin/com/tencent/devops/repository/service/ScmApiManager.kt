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

package com.tencent.devops.repository.service

import com.tencent.devops.scm.api.exception.NotFoundScmApiException
import com.tencent.devops.scm.api.pojo.BranchListOptions
import com.tencent.devops.scm.api.pojo.Change
import com.tencent.devops.scm.api.pojo.Comment
import com.tencent.devops.scm.api.pojo.CommentInput
import com.tencent.devops.scm.api.pojo.Commit
import com.tencent.devops.scm.api.pojo.CommitListOptions
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.ContentInput
import com.tencent.devops.scm.api.pojo.Hook
import com.tencent.devops.scm.api.pojo.HookInput
import com.tencent.devops.scm.api.pojo.HookRequest
import com.tencent.devops.scm.api.pojo.Issue
import com.tencent.devops.scm.api.pojo.IssueInput
import com.tencent.devops.scm.api.pojo.IssueListOptions
import com.tencent.devops.scm.api.pojo.ListOptions
import com.tencent.devops.scm.api.pojo.Oauth2AccessToken
import com.tencent.devops.scm.api.pojo.Perm
import com.tencent.devops.scm.api.pojo.PullRequest
import com.tencent.devops.scm.api.pojo.PullRequestInput
import com.tencent.devops.scm.api.pojo.PullRequestListOptions
import com.tencent.devops.scm.api.pojo.Reference
import com.tencent.devops.scm.api.pojo.ReferenceInput
import com.tencent.devops.scm.api.pojo.RepoListOptions
import com.tencent.devops.scm.api.pojo.Status
import com.tencent.devops.scm.api.pojo.StatusInput
import com.tencent.devops.scm.api.pojo.TagListOptions
import com.tencent.devops.scm.api.pojo.Tree
import com.tencent.devops.scm.api.pojo.User
import com.tencent.devops.scm.api.pojo.auth.IScmAuth
import com.tencent.devops.scm.api.pojo.repository.ScmProviderRepository
import com.tencent.devops.scm.api.pojo.repository.ScmServerRepository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import com.tencent.devops.scm.spring.manager.ScmProviderManager
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 源代码平台API接口
 */
@ScmProxy("scmApiManager")
@Service
class ScmApiManager constructor(
    private val scmProviderManager: ScmProviderManager
) {
    /*============================================repositories============================================*/
    fun findRepository(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository
    ): ScmServerRepository {
        return scmProviderManager.repositories(providerProperties).find(providerRepository)
    }

    fun findPerm(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        username: String
    ): Perm {
        return scmProviderManager.repositories(providerProperties).findPerms(providerRepository, username)
    }

    fun listRepository(
        providerProperties: ScmProviderProperties,
        auth: IScmAuth,
        opts: RepoListOptions
    ): List<ScmServerRepository> {
        return scmProviderManager.repositories(providerProperties).list(auth, opts)
    }

    fun listHooks(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: ListOptions
    ): List<Hook> {
        return scmProviderManager.repositories(providerProperties).listHooks(providerRepository, opts)
    }

    fun createHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        input: HookInput
    ): Hook {
        return scmProviderManager.repositories(providerProperties).createHook(providerRepository, input)
    }

    fun updateHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookId: Long,
        input: HookInput
    ): Hook {
        return scmProviderManager.repositories(providerProperties).updateHook(providerRepository, hookId, input)
    }

    fun getHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookId: Long
    ): Hook {
        return scmProviderManager.repositories(providerProperties).getHook(providerRepository, hookId)
    }

    fun deleteHook(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        hookId: Long
    ) {
        scmProviderManager.repositories(providerProperties).deleteHook(providerRepository, hookId)
    }

    fun listStatus(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        ref: String,
        opts: ListOptions
    ): List<Status> {
        return scmProviderManager.repositories(providerProperties).listStatus(providerRepository, ref, opts)
    }

    fun createStatus(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        ref: String,
        input: StatusInput
    ): Status {
        return scmProviderManager.repositories(providerProperties).createStatus(providerRepository, ref, input)
    }

    /*============================================refs============================================*/

    fun createBranch(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        input: ReferenceInput
    ) {
        return scmProviderManager.refs(providerProperties).createBranch(providerRepository, input)
    }

    fun findBranch(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        name: String
    ): Reference? {
        return try {
            scmProviderManager.refs(providerProperties).findBranch(providerRepository, name)
        } catch (e: NotFoundScmApiException) {
            null
        }
    }

    fun listBranches(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: BranchListOptions
    ): List<Reference> {
        return scmProviderManager.refs(providerProperties).listBranches(providerRepository, opts)
    }

    fun createTag(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        input: ReferenceInput
    ) {
        return scmProviderManager.refs(providerProperties).createTag(providerRepository, input)
    }

    fun findTag(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        name: String
    ): Reference {
        return scmProviderManager.refs(providerProperties).findTag(providerRepository, name)
    }

    fun findTags(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: TagListOptions
    ): List<Reference> {
        return scmProviderManager.refs(providerProperties).listTags(providerRepository, opts)
    }

    fun listTags(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: TagListOptions
    ): List<Reference> {
        return scmProviderManager.refs(providerProperties).listTags(providerRepository, opts)
    }

    fun findCommit(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        sha: String
    ): Commit {
        return scmProviderManager.refs(providerProperties).findCommit(providerRepository, sha)
    }

    fun listCommits(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: CommitListOptions
    ): List<Commit> {
        return scmProviderManager.refs(providerProperties).listCommits(providerRepository, opts)
    }

    fun listChanges(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        ref: String,
        opts: ListOptions
    ): List<Change> {
        return scmProviderManager.refs(providerProperties).listChanges(providerRepository, ref, opts)
    }

    fun compareChanges(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        source: String,
        target: String,
        opts: ListOptions
    ): List<Change> {
        return scmProviderManager.refs(providerProperties).compareChanges(providerRepository, source, target, opts)
    }

    /*============================================files============================================*/
    fun listFileTree(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        path: String,
        ref: String,
        recursive: Boolean
    ): List<Tree> {
        return scmProviderManager.files(providerProperties).listTree(providerRepository, path, ref, recursive)
    }

    fun getFileContent(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        path: String,
        ref: String
    ): Content? {
        return try {
            return scmProviderManager.files(providerProperties).find(providerRepository, path, ref)
        } catch (e: NotFoundScmApiException) {
            null
        }
    }

    fun createFile(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        path: String,
        input: ContentInput
    ) {
        scmProviderManager.files(providerProperties).create(providerRepository, path, input)
    }

    fun updateFile(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        path: String,
        input: ContentInput
    ) {
        scmProviderManager.files(providerProperties).update(providerRepository, path, input)
    }

    /*============================================issues============================================*/
    fun findIssue(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int
    ): Issue {
        return scmProviderManager.issues(providerProperties).find(providerRepository, number)
    }

    fun createIssue(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        input: IssueInput
    ): Issue {
        return scmProviderManager.issues(providerProperties).create(providerRepository, input)
    }

    fun listIssue(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: IssueListOptions
    ): List<Issue> {
        return scmProviderManager.issues(providerProperties).list(providerRepository, opts)
    }

    fun closeIssue(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int
    ) {
        return scmProviderManager.issues(providerProperties).close(providerRepository, number)
    }

    fun findIssueComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        commentId: Long
    ): Comment {
        return scmProviderManager.issues(providerProperties).findComment(providerRepository, number, commentId)
    }

    fun listIssueComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        opts: ListOptions
    ): List<Comment> {
        return scmProviderManager.issues(providerProperties).listComments(providerRepository, number, opts)
    }

    fun createIssueComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        input: CommentInput
    ): Comment {
        return scmProviderManager.issues(providerProperties).createComment(providerRepository, number, input)
    }

    fun deleteIssueComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        commentId: Long
    ) {
        scmProviderManager.issues(providerProperties).deleteComment(providerRepository, number, commentId)
    }

    /*============================================pull request============================================*/
    fun findPullRequest(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int
    ): PullRequest {
        return scmProviderManager.pullRequests(providerProperties).find(providerRepository, number)
    }

    fun createPullRequest(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        input: PullRequestInput
    ): PullRequest {
        return scmProviderManager.pullRequests(providerProperties).create(providerRepository, input)
    }

    fun listPullRequest(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        opts: PullRequestListOptions
    ): List<PullRequest> {
        return scmProviderManager.pullRequests(providerProperties).list(providerRepository, opts)
    }

    fun listPullRequestChanges(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        opts: ListOptions
    ): List<Change> {
        return scmProviderManager.pullRequests(providerProperties).listChanges(providerRepository, number, opts)
    }

    fun listPullRequestCommits(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        opts: ListOptions
    ): List<Commit> {
        return scmProviderManager.pullRequests(providerProperties).listCommits(providerRepository, number, opts)
    }

    fun merge(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int
    ) {
        return scmProviderManager.pullRequests(providerProperties).merge(providerRepository, number)
    }

    fun closePullRequest(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int
    ) {
        return scmProviderManager.pullRequests(providerProperties).close(providerRepository, number)
    }

    fun findPullRequestComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        commentId: Long
    ): Comment {
        return scmProviderManager.pullRequests(providerProperties).findComment(providerRepository, number, commentId)
    }

    fun listPullRequestComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        opts: ListOptions
    ): List<Comment> {
        return scmProviderManager.pullRequests(providerProperties).listComments(providerRepository, number, opts)
    }

    fun createPullRequestComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        input: CommentInput
    ): Comment {
        return scmProviderManager.pullRequests(providerProperties).createComment(providerRepository, number, input)
    }

    fun deletePullRequestComment(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        number: Int,
        commentId: Long
    ) {
        scmProviderManager.pullRequests(providerProperties).deleteComment(providerRepository, number, commentId)
    }

    /*============================================webhooks============================================*/
    fun webhookParse(
        providerProperties: ScmProviderProperties,
        request: HookRequest
    ): Webhook {
        return scmProviderManager.webhookParser(providerProperties).parse(request)
    }

    fun webhookEnrich(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository,
        webhook: Webhook
    ): Webhook {
        return scmProviderManager.webhookEnricher(providerProperties).enrich(providerRepository, webhook)
    }

    /*============================================token============================================*/
    fun authorizationUrl(
        providerProperties: ScmProviderProperties,
        state: String
    ): String {
        return scmProviderManager.token(providerProperties).authorizationUrl(state)
    }

    fun callback(
        providerProperties: ScmProviderProperties,
        code: String
    ): Oauth2AccessToken {
        return scmProviderManager.token(providerProperties).callback(code)
    }

    fun refresh(
        providerProperties: ScmProviderProperties,
        refreshToken: String
    ): Oauth2AccessToken {
        return scmProviderManager.token(providerProperties).refresh(refreshToken)
    }

    /*============================================token============================================*/
    fun getUser(
        providerProperties: ScmProviderProperties,
        auth: IScmAuth
    ): User {
        return scmProviderManager.users(providerProperties).find(auth)
    }

    /*============================================command============================================*/
    fun lsRemote(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository
    ) {
        scmProviderManager.command(providerProperties).remoteInfo(providerRepository)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ScmApiManager::class.java)
    }
}
