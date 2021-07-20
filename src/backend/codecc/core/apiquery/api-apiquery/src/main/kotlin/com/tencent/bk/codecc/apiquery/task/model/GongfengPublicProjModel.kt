package com.tencent.bk.codecc.apiquery.task.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.tencent.bk.codecc.task.jackson.ForkProjDeserializer
import com.tencent.bk.codecc.task.pojo.ConfigStorageInfo
import com.tencent.bk.codecc.task.pojo.NameSpaceInfo
import com.tencent.bk.codecc.task.pojo.OwnerInfo
import com.tencent.bk.codecc.task.pojo.StatisticsInfo

data class GongfengPublicProjModel(
    val id: Int,
    val description: String?,
    @JsonProperty("public")
    val publicProj: Boolean?,
    val archived: Boolean?,
    @JsonProperty("visibility_level")
    val visibilityLevel: Int?,
    @JsonProperty("namespace")
    val nameSpace: NameSpaceInfo?,
    var owner: OwnerInfo?,
    val name: String,
    @JsonProperty("name_with_namespace")
    val nameWithNameSpace: String,
    val path: String?,
    @JsonProperty("path_with_namespace")
    val pathWithNameSpace: String?,
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @JsonProperty("ssl_url_to_repo")
    val sshUrlToRepo: String?,
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String?,
    @JsonProperty("https_url_to_repo")
    val httpsUrlToRepo: String?,
    @JsonProperty("web_url")
    val webUrl: String?,
    @JsonProperty("tag_list")
    val tagList: List<String>?,
    @JsonProperty("issues_enabled")
    val issuesEnabled: Boolean?,
    @JsonProperty("merge_requests_enabled")
    val mergeRequestsEnabled: Boolean?,
    @JsonProperty("wiki_enabled")
    val wikiEnabled: Boolean?,
    @JsonProperty("snippets_enabled")
    val snippetsEnabled: Boolean?,
    @JsonProperty("review_enabled")
    val reviewEnabled: Boolean?,
    @JsonProperty("fork_enabled")
    val forkEnabled: Boolean?,
    @JsonProperty("tag_name_regex")
    val tagNameRegex: String?,
    @JsonProperty("tag_create_push_level")
    val tagCreatePushLevel: Int?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("last_activity_at")
    val lastActivityAt: String?,
    @JsonProperty("creator_id")
    val creatorId: Int?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("watchs_count")
    val watchsCount: Int?,
    @JsonProperty("stars_count")
    val starsCount: Int?,
    @JsonProperty("forks_count")
    val forksCount: Int?,
    @JsonProperty("config_storage")
    val configStorage: ConfigStorageInfo?,
    @JsonProperty("forked_from_project")
    @JsonDeserialize(using = ForkProjDeserializer::class)
    val forkedFromProject: ForkProjModel?,
    val statistics: StatisticsInfo?
)