package com.tencent.devops.common.sdk.github.response
import com.fasterxml.jackson.annotation.JsonProperty

data class GetAppInstallationResponse(
    @JsonProperty("access_tokens_url")
    val accessTokensUrl: String,
    @JsonProperty("app_id")
    val appId: Int,
    @JsonProperty("app_slug")
    val appSlug: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("events")
    val events: List<String>,
    @JsonProperty("has_multiple_single_files")
    val hasMultipleSingleFiles: Boolean,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("permissions")
    val permissions: GithubAppPermissions,
    @JsonProperty("repositories_url")
    val repositoriesUrl: String,
    @JsonProperty("repository_selection")
    val repositorySelection: String,
    @JsonProperty("single_file_name")
    val singleFileName: String,
    @JsonProperty("single_file_paths")
    val singleFilePaths: List<String>,
    @JsonProperty("suspended_at")
    val suspendedAt: String?,
    @JsonProperty("suspended_by")
    val suspendedBy: String?,
    @JsonProperty("target_id")
    val targetId: Int,
    @JsonProperty("target_type")
    val targetType: String,
    @JsonProperty("updated_at")
    val updatedAt: String
)

data class GithubAppPermissions(
    @JsonProperty("checks")
    val checks: String,
    @JsonProperty("contents")
    val contents: String,
    @JsonProperty("issues")
    val issues: String,
    @JsonProperty("metadata")
    val metadata: String,
    @JsonProperty("pull_requests")
    val pullRequests: String,
    @JsonProperty("single_file")
    val singleFile: String,
    @JsonProperty("statuses")
    val statuses: String
)
