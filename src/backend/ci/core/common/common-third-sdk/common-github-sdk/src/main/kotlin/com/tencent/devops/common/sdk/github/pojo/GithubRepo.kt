package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubRepo(
    @JsonProperty("allow_forking")
    val allowForking: Boolean,
    @JsonProperty("allow_merge_commit")
    val allowMergeCommit: Boolean,
    @JsonProperty("allow_rebase_merge")
    val allowRebaseMerge: Boolean,
    @JsonProperty("allow_squash_merge")
    val allowSquashMerge: Boolean,
    @JsonProperty("archive_url")
    val archiveUrl: String,
    val archived: Boolean,
    @JsonProperty("assignees_url")
    val assigneesUrl: String,
    @JsonProperty("blobs_url")
    val blobsUrl: String,
    @JsonProperty("branches_url")
    val branchesUrl: String,
    @JsonProperty("clone_url")
    val cloneUrl: String,
    @JsonProperty("collaborators_url")
    val collaboratorsUrl: String,
    @JsonProperty("comments_url")
    val commentsUrl: String,
    @JsonProperty("commits_url")
    val commitsUrl: String,
    @JsonProperty("compare_url")
    val compareUrl: String,
    @JsonProperty("contents_url")
    val contentsUrl: String,
    @JsonProperty("contributors_url")
    val contributorsUrl: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("default_branch")
    val defaultBranch: String,
    @JsonProperty("deployments_url")
    val deploymentsUrl: String,
    val description: String,
    val disabled: Boolean,
    @JsonProperty("downloads_url")
    val downloadsUrl: String,
    @JsonProperty("events_url")
    val eventsUrl: String,
    val fork: Boolean,
    val forks: Int,
    @JsonProperty("forks_count")
    val forksCount: Int,
    @JsonProperty("forks_url")
    val forksUrl: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("git_commits_url")
    val gitCommitsUrl: String,
    @JsonProperty("git_refs_url")
    val gitRefsUrl: String,
    @JsonProperty("git_tags_url")
    val gitTagsUrl: String,
    @JsonProperty("git_url")
    val gitUrl: String,
    @JsonProperty("has_downloads")
    val hasDownloads: Boolean,
    @JsonProperty("has_issues")
    val hasIssues: Boolean,
    @JsonProperty("has_pages")
    val hasPages: Boolean,
    @JsonProperty("has_projects")
    val hasProjects: Boolean,
    @JsonProperty("has_wiki")
    val hasWiki: Boolean,
    val homepage: String,
    @JsonProperty("hooks_url")
    val hooksUrl: String,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Int,
    @JsonProperty("issue_comment_url")
    val issueCommentUrl: String,
    @JsonProperty("issue_events_url")
    val issueEventsUrl: String,
    @JsonProperty("issues_url")
    val issuesUrl: String,
    @JsonProperty("keys_url")
    val keysUrl: String,
    @JsonProperty("labels_url")
    val labelsUrl: String,
    val language: String?,
    @JsonProperty("languages_url")
    val languagesUrl: String,
    val license: License,
    @JsonProperty("merges_url")
    val mergesUrl: String,
    @JsonProperty("milestones_url")
    val milestonesUrl: String,
    @JsonProperty("mirror_url")
    val mirrorUrl: String?,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    @JsonProperty("notifications_url")
    val notificationsUrl: String,
    @JsonProperty("open_issues")
    val openIssues: Int,
    @JsonProperty("open_issues_count")
    val openIssuesCount: Int,
    val owner: GithubAuthor,
    val permissions: GithubRepoPermissions?,
    val private: Boolean,
    @JsonProperty("pulls_url")
    val pullsUrl: String,
    @JsonProperty("pushed_at")
    val pushedAt: String,
    @JsonProperty("releases_url")
    val releasesUrl: String,
    val size: Int,
    @JsonProperty("ssh_url")
    val sshUrl: String,
    @JsonProperty("stargazers_count")
    val stargazersCount: Int,
    @JsonProperty("stargazers_url")
    val stargazersUrl: String,
    @JsonProperty("statuses_url")
    val statusesUrl: String,
    @JsonProperty("subscribers_url")
    val subscribersUrl: String,
    @JsonProperty("subscription_url")
    val subscriptionUrl: String,
    @JsonProperty("svn_url")
    val svnUrl: String,
    @JsonProperty("tags_url")
    val tagsUrl: String,
    @JsonProperty("teams_url")
    val teamsUrl: String,
    @JsonProperty("temp_clone_token")
    val tempCloneToken: String?,
    val topics: List<String>,
    @JsonProperty("trees_url")
    val treesUrl: String,
    @JsonProperty("updated_at")
    val updatedAt: String,
    val url: String,
    val watchers: Int,
    @JsonProperty("watchers_count")
    val watchersCount: Int
)
