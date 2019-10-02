package com.tencent.devops.process.pojo.scm.code.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPullRequestEvent(
    val action: String,
    val number: Long,
    val pull_request: GithubPullRequest,
    val repository: GithubRepository,
    override val sender: GithubSender
) : GithubEvent(sender) {
    companion object {
        const val classType = "pull_request"
    }
}

data class GithubPullRequest(
    val id: Long,
    val url: String,
    val head: GithubPullRequestBranch,
    val base: GithubPullRequestBranch,
    val labels: List<GithubLabel>,
    val comments_url: String?,
    val created_at: String?,
    val update_at: String?,
    val assignees: List<GithubAssignee>,
    val milestone: GithubMilestone?,
    val title: String?,
    val requested_reviewers: List<GithubReviewer>
)

data class GithubPullRequestBranch(
    val ref: String,
    val sha: String,
    val repo: GithubRepo
)

data class GithubRepo(
    val name: String,
    val full_name: String,
    val clone_url: String
)

data class GithubAssignee(
    val login: String?
)

data class GithubReviewer(
    val login: String?
)

data class GithubMilestone(
    val title: String?,
    val due_on: String?
)

data class GithubLabel(
    val name: String
)
