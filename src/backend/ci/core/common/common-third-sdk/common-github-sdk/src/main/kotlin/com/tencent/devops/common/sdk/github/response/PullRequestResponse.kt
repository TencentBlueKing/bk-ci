package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.GithubCommitPointer
import com.tencent.devops.common.sdk.github.pojo.GithubMilestone
import com.tencent.devops.common.sdk.github.pojo.GithubTeam
import com.tencent.devops.common.sdk.github.pojo.GithubUser
import com.tencent.devops.common.sdk.github.pojo.PullRequestLabel

@JsonIgnoreProperties(ignoreUnknown = true)
data class PullRequestResponse(
    val url: String,
    val id: Int,
    @JsonProperty("node_id")
    val nodeId: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("diff_url")
    val diffUrl: String,
    @JsonProperty("patch_url")
    val patchUrl: String,
    @JsonProperty("issue_url")
    val issueUrl: String,
    @JsonProperty("commits_url")
    val commitsUrl: String,
    @JsonProperty("review_comments_url")
    val reviewCommentsUrl: String,
    @JsonProperty("review_comment_url")
    val reviewCommentUrl: String,
    @JsonProperty("comments_url")
    val commentsUrl: String,
    @JsonProperty("statuses_url")
    val statusesUrl: String,
    val number: Int,
    val state: String,
    val locked: Boolean,
    val title: String,
    val user: GithubUser,
    val body: String?,
    val labels: List<PullRequestLabel>,
    val milestone: GithubMilestone?,
    @JsonProperty("active_lock_reason")
    val activeLockReason: String?,
    @JsonProperty("create_at")
    val createdAt: String?,
    @JsonProperty("update_at")
    val updatedAt: String?,
    @JsonProperty("closed_at")
    val closedAt: String?,
    @JsonProperty("merged_at")
    val mergedAt: String?,
    @JsonProperty("merge_commit_sha")
    val mergeCommitSha: String?,
    val assignee: GithubUser?,
    val assignees: List<GithubUser>,
    @JsonProperty("requested_reviewers")
    val requestedReviewers: List<GithubUser>,
    @JsonProperty("requested_teams")
    val requestedTeams: List<GithubTeam>,
    val head: GithubCommitPointer,
    val base: GithubCommitPointer,
    @JsonProperty("author_association")
    val authorAssociation: String,
    @JsonProperty("auto_merge")
    val autoMerge: String?,
    val draft: Boolean,
    val merged: Boolean,
    val mergeable: Boolean?,
    val rebaseable: Boolean,
    @JsonProperty("mergeable_state")
    val mergeableState: String,
    @JsonProperty("merged_by")
    val mergedBy: GithubUser?,
    val comments: Int,
    @JsonProperty("review_comments")
    val reviewComments: Int,
    @JsonProperty("maintainer_can_modify")
    val maintainerCanModify: Boolean,
    val commits: Int,
    val additions: Int,
    val deletions: Int,
    @JsonProperty("changed_files")
    val changedFiles: Int
)
