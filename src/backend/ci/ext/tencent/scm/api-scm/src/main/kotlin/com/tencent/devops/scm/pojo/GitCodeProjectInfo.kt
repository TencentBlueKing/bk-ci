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

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/**
 {
    "id": 8922,
    "description": null,
    "public": false,
    "archived": false,
    "visibility_level": 0,
    "public_visibility": 100,
    "namespace": {
        "created_at": "2017-01-16T08:29:43+0000",
        "description": "git_user1",
        "id": 11323,
        "name": "git_user1",
        "owner_id": 11323,
        "path": "git_user1",
        "updated_at": "2017-01-16T08:29:43+0000"
    },
    "owner": {
        "id": 11323,
        "username": "git_user1",
        "web_url": "http://git.example.tencent.com/u/git_user1",
        "name": "git_user1",
        "state": "active",
        "avatar_url": "git.example.tencent.com/uploads/user/avatar/11323/a75ba2727c7a409cab1d15dd993149aa.jpg"
    },
    "name": "test-01",
    "name_with_namespace": "git_user1/test-01",
    "path": "test-01",
    "path_with_namespace": "git_user1/test-01",
    "default_branch": "master",
    "ssh_url_to_repo": "git@git.example.tencent.com:git_user1/test-01.git",
    "http_url_to_repo": "http://git.example.tencent.com/git_user1/test-01.git",
    "https_url_to_repo": "https://git.example.tencent.com/git_user1/test-01.git",
    "web_url": "http://git.example.tencent.com/git_user1/test-01",
    "tag_list": [],
    "issues_enabled": true,
    "merge_requests_enabled": true,
    "wiki_enabled": true,
    "snippets_enabled": true,
    "review_enabled": true,
    "fork_enabled": false,
    "tag_name_regex": null,
    "tag_create_push_level": 30,
    "created_at": "2017-08-13T07:37:14+0000",
    "last_activity_at": "2017-08-13T07:37:14+0000",
    "creator_id": 11323,
    "avatar_url": "http://git.example.tencent.com/uploads/project/avatar/70703",
    "watchs_count": 0,
    "stars_count": 0,
    "forks_count": 0,
    "config_storage": {
        "limit_lfs_file_size": 500,
        "limit_size": 100000,
        "limit_file_size": 100000,
        "limit_lfs_size": 100000
    },
    "forked_from_project": "Forked Project not found",
    "statistics": {
        "commit_count": 0,
        "repository_size": 0
    },
    "suggestion_reviewers": {
        "id": 80041,
        "username": "git_user1",
        "web_url": "http://git.example.tencent.com/u/git_user1",
        "name": "git_user1",
        "state": "active",
        "avatar_url": "http://git.example.tencent.com/assets/images/avatar/no_user_avatar.png"
    },
    "necessary_reviewers": [],
    "path_reviewer_rules": "",
    "approver_rule": 1,
    "necessary_approver_rule": 0,
    "can_approve_by_creator": true,
    "auto_create_review_after_push": true,
    "push_reset_enabled": false,
    "merge_request_template": null,
    "file_owner_path_rules": "*.js review owner=username1,username2"
}
 */

@ApiModel("工蜂项目详细信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCodeProjectInfo(
    @JsonProperty("approver_rule")
    val approverRule: Int?,
    @JsonProperty("archived")
    val archived: Boolean?,
    @JsonProperty("auto_create_review_after_push")
    val autoCreateReviewAfterPush: Boolean?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("can_approve_by_creator")
    val canApproveByCreator: Boolean?,
    @JsonProperty("config_storage")
    val configStorage: ConfigStorage?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("creator_id")
    val creatorId: Int?,
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @JsonProperty("description")
    val description: Any?,
    @JsonProperty("file_owner_path_rules")
    val fileOwnerPathRules: String?,
    @JsonProperty("fork_enabled")
    val forkEnabled: Boolean?,
    @JsonProperty("forked_from_project")
    val forkedFromProject: String?,
    @JsonProperty("forks_count")
    val forksCount: Int?,
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String?,
    @JsonProperty("https_url_to_repo")
    val httpsUrlToRepo: String?,
    @JsonProperty("id")
    val id: Int?,
    @JsonProperty("issues_enabled")
    val issuesEnabled: Boolean?,
    @JsonProperty("last_activity_at")
    val lastActivityAt: String?,
    @JsonProperty("merge_request_template")
    val mergeRequestTemplate: Any?,
    @JsonProperty("merge_requests_enabled")
    val mergeRequestsEnabled: Boolean?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("name_with_namespace")
    val nameWithNamespace: String?,
    @JsonProperty("namespace")
    val namespace: Namespace?,
    @JsonProperty("necessary_approver_rule")
    val necessaryApproverRule: Int?,
    @JsonProperty("necessary_reviewers")
    val necessaryReviewers: List<Any>?,
    @JsonProperty("owner")
    val owner: Owner?,
    @JsonProperty("path")
    val path: String?,
    @JsonProperty("path_reviewer_rules")
    val pathReviewerRules: String?,
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String?,
    @JsonProperty("public")
    val public: Boolean?,
    @JsonProperty("public_visibility")
    val publicVisibility: Int?,
    @JsonProperty("push_reset_enabled")
    val pushResetEnabled: Boolean?,
    @JsonProperty("review_enabled")
    val reviewEnabled: Boolean?,
    @JsonProperty("snippets_enabled")
    val snippetsEnabled: Boolean?,
    @JsonProperty("ssh_url_to_repo")
    val sshUrlToRepo: String?,
    @JsonProperty("stars_count")
    val starsCount: Int?,
    @JsonProperty("statistics")
    val statistics: Statistics?,
    @JsonProperty("suggestion_reviewers")
    val suggestionReviewers: SuggestionReviewers?,
    @JsonProperty("tag_create_push_level")
    val tagCreatePushLevel: Int?,
    @JsonProperty("tag_list")
    val tagList: List<Any>?,
    @JsonProperty("tag_name_regex")
    val tagNameRegex: Any?,
    @JsonProperty("visibility_level")
    val visibilityLevel: Int?,
    @JsonProperty("watchs_count")
    val watchsCount: Int?,
    @JsonProperty("web_url")
    val webUrl: String?,
    @JsonProperty("wiki_enabled")
    val wikiEnabled: Boolean?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SuggestionReviewers(
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("id")
    val id: Int?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("state")
    val state: String?,
    @JsonProperty("username")
    val username: String?,
    @JsonProperty("web_url")
    val webUrl: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Statistics(
    @JsonProperty("commit_count")
    val commitCount: Int?,
    @JsonProperty("repository_size")
    val repositorySize: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Owner(
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("id")
    val id: Int?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("state")
    val state: String?,
    @JsonProperty("username")
    val username: String?,
    @JsonProperty("web_url")
    val webUrl: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Namespace(
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("id")
    val id: Int?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("owner_id")
    val ownerId: Int?,
    @JsonProperty("path")
    val path: String?,
    @JsonProperty("updated_at")
    val updatedAt: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ConfigStorage(
    @JsonProperty("limit_file_size")
    val limitFileSize: Int?,
    @JsonProperty("limit_lfs_file_size")
    val limitLfsFileSize: Int?,
    @JsonProperty("limit_lfs_size")
    val limitLfsSize: Int?,
    @JsonProperty("limit_size")
    val limitSize: Int?
)
