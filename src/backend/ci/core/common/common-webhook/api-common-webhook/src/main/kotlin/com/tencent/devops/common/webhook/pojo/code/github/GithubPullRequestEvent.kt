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

package com.tencent.devops.common.webhook.pojo.code.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPullRequestEvent(
    @JsonProperty("action")
    val action: String, // opened
    @JsonProperty("number")
    val number: Int, // 1
    @JsonProperty("pull_request")
    val pullRequest: GithubPullRequest,
    @JsonProperty("repository")
    val repository: GithubRepository,
    @JsonProperty("sender")
    override val sender: GithubUser
) : GithubEvent(sender) {
    companion object {
        const val classType = "pull_request"
    }
}

fun GithubPullRequestEvent.isPrForkNotMergeEvent() =
    this.pullRequest.merged == false && this.isPrForkEvent()

fun GithubPullRequestEvent.isPrForkEvent() = this.pullRequest.base.repo.id != this.pullRequest.head.repo.id

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPullRequest(
    @JsonProperty("active_lock_reason")
    val activeLockReason: String?, // null
    @JsonProperty("additions")
    val additions: Int, // 1
    @JsonProperty("assignee")
    val assignee: GithubUser?,
    @JsonProperty("assignees")
    val assignees: List<GithubUser>,
    @JsonProperty("author_association")
    val authorAssociation: String, // OWNER
    @JsonProperty("auto_merge")
    val autoMerge: String?, // null
    @JsonProperty("base")
    val base: GithubPullRequestBranch, // 目标分支
    @JsonProperty("body")
    val body: String?, // testestetsets
    @JsonProperty("changed_files")
    val changedFiles: Int, // 1
    @JsonProperty("closed_at")
    val closedAt: String?, // null
    @JsonProperty("comments")
    val comments: Int, // 0
    @JsonProperty("comments_url")
    val commentsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/issues/1/comments
    @JsonProperty("commits")
    val commits: Int, // 1
    @JsonProperty("commits_url")
    val commitsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/pulls/1/commits
    @JsonProperty("created_at")
    val createdAt: String, // 2022-06-21T08:45:41Z
    @JsonProperty("deletions")
    val deletions: Int, // 0
    @JsonProperty("diff_url")
    val diffUrl: String, // https://github.com/yongyiduan/webhook-test/pull/1.diff
    @JsonProperty("draft")
    val draft: Boolean, // false
    @JsonProperty("head")
    val head: GithubPullRequestBranch, // 源分支
    @JsonProperty("html_url")
    val htmlUrl: String, // https://github.com/yongyiduan/webhook-test/pull/1
    @JsonProperty("id")
    val id: Int, // 973279061
    @JsonProperty("issue_url")
    val issueUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/issues/1
    @JsonProperty("labels")
    val labels: List<GithubLabel>,
    @JsonProperty("locked")
    val locked: Boolean, // false
    @JsonProperty("maintainer_can_modify")
    val maintainerCanModify: Boolean, // false
    @JsonProperty("merge_commit_sha")
    val mergeCommitSha: String?, // null
    @JsonProperty("mergeable")
    val mergeable: String?, // null
    @JsonProperty("mergeable_state")
    val mergeableState: String, // unknown
    @JsonProperty("merged")
    val merged: Boolean, // false
    @JsonProperty("merged_at")
    val mergedAt: String?, // null
    @JsonProperty("merged_by")
    val mergedBy: GithubUser?, // null
    @JsonProperty("milestone")
    val milestone: GithubMilestone?, // null
    @JsonProperty("node_id")
    val nodeId: String, // PR_kwDOHiUKK846Aw9V
    @JsonProperty("number")
    val number: Int, // 1
    @JsonProperty("patch_url")
    val patchUrl: String, // https://github.com/yongyiduan/webhook-test/pull/1.patch
    @JsonProperty("rebaseable")
    val rebaseable: String?, // null
    @JsonProperty("requested_reviewers")
    val requestedReviewers: List<GithubUser>,
    @JsonProperty("requested_teams")
    val requestedTeams: List<Any>,
    @JsonProperty("review_comment_url")
    val reviewCommentUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/pulls/comments{/number}
    @JsonProperty("review_comments")
    val reviewComments: Int, // 0
    @JsonProperty("review_comments_url")
    val reviewCommentsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/pulls/1/comments
    @JsonProperty("state")
    val state: String, // open
    @JsonProperty("statuses_url")
    val statusesUrl: String,
    @JsonProperty("title")
    val title: String, // Update 1 testesteste
    @JsonProperty("updated_at")
    val updatedAt: String?, // 2022-06-21T08:45:42Z
    @JsonProperty("url")
    val url: String, // https://api.github.com/repos/yongyiduan/webhook-test/pulls/1
    @JsonProperty("user")
    val user: GithubUser
)

data class GithubMilestone(
    @JsonProperty("closed_at")
    val closedAt: String?, // 2013-02-12T13:22:01Z
    @JsonProperty("closed_issues")
    val closedIssues: Int, // 8
    @JsonProperty("created_at")
    val createdAt: String, // 2011-04-10T20:09:31Z
    @JsonProperty("creator")
    val creator: GithubUser,
    @JsonProperty("description")
    val description: String, // Tracking milestone for version 1.0
    @JsonProperty("due_on")
    val dueOn: String?, // 2012-10-09T23:39:01Z
    @JsonProperty("html_url")
    val htmlUrl: String, // https://github.com/octocat/Hello-World/milestones/v1.0
    @JsonProperty("id")
    val id: Int, // 1002604
//    @JsonProperty("labels_url")
//    val labelsUrl: String, // https://api.github.com/repos/octocat/Hello-World/milestones/1/labels
    @JsonProperty("node_id")
    val nodeId: String, // MDk6TWlsZXN0b25lMTAwMjYwNA==
    @JsonProperty("number")
    val number: Int, // 1
    @JsonProperty("open_issues")
    val openIssues: Int, // 4
    @JsonProperty("state")
    val state: String, // open
    @JsonProperty("title")
    val title: String, // v1.0
    @JsonProperty("updated_at")
    val updatedAt: String? // 2014-03-03T18:58:10Z
//    @JsonProperty("url")
//    val url: String // https://api.github.com/repos/octocat/Hello-World/milestones/1
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubLabel(
    @JsonProperty("color")
    val color: String, // 0075ca
    @JsonProperty("default")
    val default: Boolean, // true
    @JsonProperty("description")
    val description: String, // Improvements or additions to documentation
    @JsonProperty("id")
    val id: Long, // 4253282496
    @JsonProperty("name")
    val name: String, // documentation
    @JsonProperty("node_id")
    val nodeId: String, // LA_kwDOHiUKK879g_DA
    @JsonProperty("url")
    val url: String // https://api.github.com/repos/yongyiduan/webhook-test/labels/documentation
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPullRequestBranch(
    @JsonProperty("label")
    val label: String, // yongyiduan:main
    @JsonProperty("ref")
    val ref: String, // main
    @JsonProperty("repo")
    val repo: GithubRepository,
    @JsonProperty("sha")
    val sha: String, // 5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
    @JsonProperty("user")
    val user: GithubUser
)
