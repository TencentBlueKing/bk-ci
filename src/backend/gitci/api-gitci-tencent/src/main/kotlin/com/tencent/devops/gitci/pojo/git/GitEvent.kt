package com.tencent.devops.gitci.pojo.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "object_kind")
@JsonSubTypes(
        JsonSubTypes.Type(value = GitPushEvent::class, name = GitPushEvent.classType),
        JsonSubTypes.Type(value = GitTagPushEvent::class, name = GitTagPushEvent.classType),
        JsonSubTypes.Type(value = GitMergeRequestEvent::class, name = GitMergeRequestEvent.classType)
)
abstract class GitEvent

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
data class GitCommitAuthor(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitUser(
    val name: String,
    val username: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitProject(
    val name: String,
    val ssh_url: String,
    val http_url: String,
    val web_url: String,
    val namespace: String,
    val visibility_level: Int
)