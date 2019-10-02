package com.tencent.devops.process.pojo.scm.code.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class GithubEvent(
    open val sender: GithubSender
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommit(
    val id: String,
    val timestamp: String,
    val url: String,
    val message: String,
    val author: GithubUser,
    val committer: GithubUser
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubUser(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubPusher(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubRepository(
    val name: String,
    val full_name: String,
    val git_url: String,
    val ssh_url: String,
    val clone_url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubSender(
    val login: String,
    val type: String,
    val avatar_url: String
)