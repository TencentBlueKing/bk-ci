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

package com.tencent.devops.common.webhook.pojo.code.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "object_kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = GitPushEvent::class, name = GitPushEvent.classType),
    JsonSubTypes.Type(value = GitTagPushEvent::class, name = GitTagPushEvent.classType),
    JsonSubTypes.Type(value = GitMergeRequestEvent::class, name = GitMergeRequestEvent.classType),
    JsonSubTypes.Type(value = GitIssueEvent::class, name = GitIssueEvent.classType),
    JsonSubTypes.Type(value = GitReviewEvent::class, name = GitReviewEvent.classType),
    JsonSubTypes.Type(value = GitNoteEvent::class, name = GitNoteEvent.classType)
)
abstract class GitEvent : CodeWebhookEvent

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCommitRepository(
    val name: String,
    val url: String,
    val git_http_url: String,
    val git_ssh_url: String,
    val homepage: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCommit(
    val id: String,
    val message: String,
    val timestamp: String,
    val author: GitCommitAuthor,
    val modified: List<String>?,
    val added: List<String>?,
    val removed: List<String>?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitDiffFile(
    @JsonProperty("new_path")
    val newPath: String,
    @JsonProperty("old_path")
    val oldPath: String,
    @JsonProperty("a_mode")
    val aMode: Long,
    @JsonProperty("b_mode")
    val bMode: Long,
    @JsonProperty("new_file")
    val newFile: Boolean,
    @JsonProperty("renamed_file")
    val renamedFile: Boolean,
    @JsonProperty("deleted_file")
    val deletedFile: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCommitAuthor(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitUser(
    val name: String,
    val username: String
)

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitProject(
    val name: String,
    val ssh_url: String,
    val http_url: String,
    val web_url: String,
    val namespace: String,
    val visibility_level: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitRepository(
    val name: String,
    val url: String,
    val description: String? = null,
    val homepage: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitChangeFileInfo(
    @JsonProperty("old_path")
    val oldPath: String?,
    @JsonProperty("new_path")
    val newPath: String?,
    @JsonProperty("a_mode")
    val aMode: Long?,
    @JsonProperty("b_mode")
    val bMode: Long?,
    @JsonProperty("new_file")
    val newFile: Boolean?,
    @JsonProperty("renamed_file")
    val renameFile: Boolean?,
    @JsonProperty("deleted_file")
    val deletedFile: Boolean?,
    val diff: String?
)

fun GitEvent.isDeleteEvent() = (this is GitPushEvent && this.isDeleteBranch()) ||
    (this is GitTagPushEvent && this.isDeleteTag())

fun GitEvent.isMrMergeEvent() = this is GitMergeRequestEvent && this.isMrMergeEvent()

fun GitEvent.isMrForkEvent() = this is GitMergeRequestEvent && this.isMrForkEvent()

fun GitEvent.isMrForkNotMergeEvent() = !this.isMrMergeEvent() && this.isMrForkEvent()
