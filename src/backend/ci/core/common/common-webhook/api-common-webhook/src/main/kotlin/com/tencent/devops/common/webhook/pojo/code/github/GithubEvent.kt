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
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class GithubEvent(
    open val sender: GithubUser
) : CodeWebhookEvent

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommit(
    @JsonProperty("added")
    val added: List<String>,
    @JsonProperty("author")
    val author: GithubAuthor,
    @JsonProperty("committer")
    val committer: GithubCommitter,
    @JsonProperty("distinct")
    val distinct: Boolean, // true
    @JsonProperty("id")
    val id: String, // 5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
    @JsonProperty("message")
    val message: String, // Update 1
    @JsonProperty("modified")
    val modified: List<String>,
    @JsonProperty("removed")
    val removed: List<String>,
    @JsonProperty("timestamp")
    val timestamp: String, // 2022-06-21T15:57:41+08:00
    @JsonProperty("tree_id")
    val treeId: String, // 1472bcfdd03256946d61aabba06b8dc12c739007
    @JsonProperty("url")
    val url: String // https://github.com/yongyiduan/webhook-test/commit/5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitter(
    @JsonProperty("email")
    val email: String, // noreply@github.com
    @JsonProperty("name")
    val name: String, // GitHub
    @JsonProperty("username")
    val username: String? // web-flow
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubUser(
//    @JsonProperty("avatar_url")
//    val avatarUrl: String, // https://avatars.githubusercontent.com/u/88175075?v=4
//    @JsonProperty("email")
//    val email: String, // 88175075+yongyiduan@users.noreply.github.com
//    @JsonProperty("events_url")
//    val eventsUrl: String, // https://api.github.com/users/yongyiduan/events{/privacy}
//    @JsonProperty("followers_url")
//    val followersUrl: String, // https://api.github.com/users/yongyiduan/followers
//    @JsonProperty("following_url")
//    val followingUrl: String, // https://api.github.com/users/yongyiduan/following{/other_user}
//    @JsonProperty("gists_url")
//    val gistsUrl: String, // https://api.github.com/users/yongyiduan/gists{/gist_id}
    @JsonProperty("gravatar_id")
    val gravatarId: String,
//    @JsonProperty("html_url")
//    val htmlUrl: String, // https://github.com/yongyiduan
    @JsonProperty("id")
    val id: Int, // 88175075
    @JsonProperty("login")
    val login: String, // yongyiduan
    @JsonProperty("node_id")
    val nodeId: String, // MDQ6VXNlcjg4MTc1MDc1
//    @JsonProperty("organizations_url")
//    val organizationsUrl: String, // https://api.github.com/users/yongyiduan/orgs
//    @JsonProperty("received_events_url")
//    val receivedEventsUrl: String, // https://api.github.com/users/yongyiduan/received_events
//    @JsonProperty("repos_url")
//    val reposUrl: String, // https://api.github.com/users/yongyiduan/repos
    @JsonProperty("site_admin")
    val siteAdmin: Boolean, // false
//    @JsonProperty("starred_url")
//    val starredUrl: String, // https://api.github.com/users/yongyiduan/starred{/owner}{/repo}
//    @JsonProperty("subscriptions_url")
//    val subscriptionsUrl: String, // https://api.github.com/users/yongyiduan/subscriptions
    @JsonProperty("type")
    val type: String // User
//    @JsonProperty("url")
//    val url: String // https://api.github.com/users/yongyiduan
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPusher(
    @JsonProperty("email")
    val email: String, // 88175075+yongyiduan@users.noreply.github.com
    @JsonProperty("name")
    val name: String // yongyiduan
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubRepository(
    @JsonProperty("allow_auto_merge")
    val allowAutoMerge: Boolean?, // false // only in pr
    @JsonProperty("allow_merge_commit")
    val allowMergeCommit: Boolean?, // true // only in pr
    @JsonProperty("allow_rebase_merge")
    val allowRebaseMerge: Boolean?, // true // only in pr
    @JsonProperty("allow_squash_merge")
    val allowSquashMerge: Boolean?, // true // only in pr
    @JsonProperty("allow_update_branch")
    val allowUpdateBranch: Boolean?, // false // only in pr
    @JsonProperty("delete_branch_on_merge")
    val deleteBranchOnMerge: Boolean?, // false // only in pr
    @JsonProperty("use_squash_pr_title_as_default")
    val useSquashPrTitleAsDefault: Boolean?, // false // only in pr
    @JsonProperty("allow_forking")
    val allowForking: Boolean, // true
//    @JsonProperty("archive_url")
//    val archiveUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/{archive_format}{/ref}
    @JsonProperty("archived")
    val archived: Boolean, // false
//    @JsonProperty("assignees_url")
//    val assigneesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/assignees{/user}
//    @JsonProperty("blobs_url")
//    val blobsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/git/blobs{/sha}
//    @JsonProperty("branches_url")
//    val branchesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/branches{/branch}
    @JsonProperty("clone_url")
    val cloneUrl: String, // https://github.com/yongyiduan/webhook-test.git
//    @JsonProperty("collaborators_url")
//    val collaboratorsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/collaborators{/collaborator}
//    @JsonProperty("comments_url")
//    val commentsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/comments{/number}
//    @JsonProperty("commits_url")
//    val commitsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/commits{/sha}
//    @JsonProperty("compare_url")
//    val compareUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/compare/{base}...{head}
//    @JsonProperty("contents_url")
//    val contentsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/contents/{+path}
//    @JsonProperty("contributors_url")
//    val contributorsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/contributors
    @JsonProperty("created_at")
    val createdAt: Any, // 1655798261 or 2022-06-21T08:45:42Z github这两种类型都会传,难受~
    @JsonProperty("default_branch")
    val defaultBranch: String, // main
//    @JsonProperty("deployments_url")
//    val deploymentsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/deployments
    @JsonProperty("description")
    val description: String?, // null
    @JsonProperty("disabled")
    val disabled: Boolean, // false
//    @JsonProperty("downloads_url")
//    val downloadsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/downloads
//    @JsonProperty("events_url")
//    val eventsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/events
    @JsonProperty("fork")
    val fork: Boolean, // false
    @JsonProperty("forks")
    val forks: Int, // 0
    @JsonProperty("forks_count")
    val forksCount: Int, // 0
//    @JsonProperty("forks_url")
//    val forksUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/forks
    @JsonProperty("full_name")
    val fullName: String, // yongyiduan/webhook-test
    @JsonProperty("git_commits_url")
//    val gitCommitsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/git/commits{/sha}
//    @JsonProperty("git_refs_url")
//    val gitRefsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/git/refs{/sha}
//    @JsonProperty("git_tags_url")
//    val gitTagsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/git/tags{/sha}
//    @JsonProperty("git_url")
    val gitUrl: String, // git://github.com/yongyiduan/webhook-test.git
    @JsonProperty("has_downloads")
    val hasDownloads: Boolean, // true
    @JsonProperty("has_issues")
    val hasIssues: Boolean, // true
    @JsonProperty("has_pages")
    val hasPages: Boolean, // false
    @JsonProperty("has_projects")
    val hasProjects: Boolean, // true
    @JsonProperty("has_wiki")
    val hasWiki: Boolean, // true
    @JsonProperty("homepage")
    val homepage: String?, // null
//    @JsonProperty("hooks_url")
//    val hooksUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/hooks
//    @JsonProperty("html_url")
//    val htmlUrl: String, // https://github.com/yongyiduan/webhook-test
    @JsonProperty("id")
    val id: Int, // 505743915
    @JsonProperty("is_template")
    val isTemplate: Boolean, // false
//    @JsonProperty("issue_comment_url")
//    val issueCommentUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/issues/comments{/number}
//    @JsonProperty("issue_events_url")
//    val issueEventsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/issues/events{/number}
//    @JsonProperty("issues_url")
//    val issuesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/issues{/number}
//    @JsonProperty("keys_url")
//    val keysUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/keys{/key_id}
//    @JsonProperty("labels_url")
//    val labelsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/labels{/name}
    @JsonProperty("language")
    val language: String?, // null
    @JsonProperty("master_branch")
    val masterBranch: String?, // main
//    @JsonProperty("merges_url")
//    val mergesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/merges
//    @JsonProperty("milestones_url")
//    val milestonesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/milestones{/number}
//    @JsonProperty("mirror_url")
//    val mirrorUrl: String?, // null
    @JsonProperty("name")
    val name: String, // webhook-test
    @JsonProperty("node_id")
    val nodeId: String, // R_kgDOHiUKKw
//    @JsonProperty("notifications_url")
//    val notificationsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/notifications{?since,all,participating}
    @JsonProperty("open_issues")
    val openIssues: Int, // 0
    @JsonProperty("open_issues_count")
    val openIssuesCount: Int, // 0
    @JsonProperty("owner")
    val owner: GithubUser,
    @JsonProperty("private")
    val checkPrivate: Boolean, // false
//    @JsonProperty("pulls_url")
//    val pullsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/pulls{/number}
    @JsonProperty("pushed_at")
    val pushedAt: Any, // 1655798261 or 2022-06-21T08:45:42Z github这两种类型都会传,难受~
//    @JsonProperty("releases_url")
//    val releasesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/releases{/id}
    @JsonProperty("size")
    val size: Int, // 0
    @JsonProperty("ssh_url")
    val sshUrl: String, // git@github.com:yongyiduan/webhook-test.git
    @JsonProperty("stargazers")
    val stargazers: Int?, // 0
    @JsonProperty("stargazers_count")
    val stargazersCount: Int, // 0
//    @JsonProperty("stargazers_url")
//    val stargazersUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/stargazers
//    @JsonProperty("statuses_url")
//    val statusesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/statuses/{sha}
//    @JsonProperty("subscribers_url")
//    val subscribersUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/subscribers
//    @JsonProperty("subscription_url")
//    val subscriptionUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/subscription
//    @JsonProperty("svn_url")
//    val svnUrl: String, // https://github.com/yongyiduan/webhook-test
//    @JsonProperty("tags_url")
//    val tagsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/tags
//    @JsonProperty("teams_url")
//    val teamsUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/teams
    @JsonProperty("topics")
    val topics: List<Any>,
//    @JsonProperty("trees_url")
//    val treesUrl: String, // https://api.github.com/repos/yongyiduan/webhook-test/git/trees{/sha}
    @JsonProperty("updated_at")
    val updatedAt: String?, // 2022-06-21T07:54:56Z
    @JsonProperty("url")
    val url: String, // https://github.com/yongyiduan/webhook-test
    @JsonProperty("visibility")
    val visibility: String, // public
    @JsonProperty("watchers")
    val watchers: Int, // 0
    @JsonProperty("watchers_count")
    val watchersCount: Int // 0
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubAuthor(
    @JsonProperty("email")
    val email: String, // 88175075+yongyiduan@users.noreply.github.com
    @JsonProperty("name")
    val name: String, // yongyiduan
    @JsonProperty("username")
    val username: String? // yongyiduan
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubHeadCommit(
    @JsonProperty("added")
    val added: List<String>,
    @JsonProperty("author")
    val author: GithubAuthor,
    @JsonProperty("committer")
    val committer: GithubCommitter,
    @JsonProperty("distinct")
    val distinct: Boolean, // true
    @JsonProperty("id")
    val id: String, // 5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
    @JsonProperty("message")
    val message: String, // Update 1
    @JsonProperty("modified")
    val modified: List<String>,
    @JsonProperty("removed")
    val removed: List<String>,
    @JsonProperty("timestamp")
    val timestamp: String, // 2022-06-21T15:57:41+08:00
    @JsonProperty("tree_id")
    val treeId: String, // 1472bcfdd03256946d61aabba06b8dc12c739007
    @JsonProperty("url")
    val url: String // https://github.com/yongyiduan/webhook-test/commit/5f76b8875980ba0fc13fce4fe0866ba7dae9c9f9
)
