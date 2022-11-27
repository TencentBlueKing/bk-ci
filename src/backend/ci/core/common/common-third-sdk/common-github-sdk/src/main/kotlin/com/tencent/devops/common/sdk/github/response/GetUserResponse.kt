package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GetUserResponse(
    @JsonProperty("avatar_url")
    val avatarUrl: String,
    @JsonProperty("bio")
    val bio: String?,
    @JsonProperty("blog")
    val blog: String,
    @JsonProperty("collaborators")
    val collaborators: Int,
    @JsonProperty("company")
    val company: String?,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("disk_usage")
    val diskUsage: Int,
    @JsonProperty("email")
    val email: String?,
    @JsonProperty("events_url")
    val eventsUrl: String,
    @JsonProperty("followers")
    val followers: Int,
    @JsonProperty("followers_url")
    val followersUrl: String,
    @JsonProperty("following")
    val following: Int,
    @JsonProperty("following_url")
    val followingUrl: String,
    @JsonProperty("gists_url")
    val gistsUrl: String,
    @JsonProperty("gravatar_id")
    val gravatarId: String,
    @JsonProperty("hireable")
    val hireable: Boolean,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("location")
    val location: String?,
    @JsonProperty("login")
    val login: String,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("node_id")
    val nodeId: String,
    @JsonProperty("organizations_url")
    val organizationsUrl: String,
    @JsonProperty("owned_private_repos")
    val ownedPrivateRepos: Int,
    @JsonProperty("private_gists")
    val privateGists: Int,
    @JsonProperty("public_gists")
    val publicGists: Int,
    @JsonProperty("public_repos")
    val publicRepos: Int,
    @JsonProperty("received_events_url")
    val receivedEventsUrl: String,
    @JsonProperty("repos_url")
    val reposUrl: String,
    @JsonProperty("site_admin")
    val siteAdmin: Boolean,
    @JsonProperty("starred_url")
    val starredUrl: String,
    @JsonProperty("subscriptions_url")
    val subscriptionsUrl: String,
    @JsonProperty("total_private_repos")
    val totalPrivateRepos: Int,
    @JsonProperty("twitter_username")
    val twitterUsername: String?,
    @JsonProperty("two_factor_authentication")
    val twoFactorAuthentication: Boolean,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("updated_at")
    val updatedAt: String?,
    @JsonProperty("url")
    val url: String
)
