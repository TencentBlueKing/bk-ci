package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfo(
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String?,
    @JsonProperty("https_url_to_repo")
    val httpsUrlToRepo: String?,
    val permissions: TGitProjectInfoPermissions?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfoPermissions(
    @JsonProperty("project_access")
    val projectAccess: TGitProjectInfoPermissionsAccess?,
    @JsonProperty("share_group_access")
    val shareGroupAccess: TGitProjectInfoPermissionsAccess?,
    @JsonProperty("group_access")
    val groupAccess: TGitProjectInfoPermissionsAccess?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfoPermissionsAccess(
    @JsonProperty("access_level")
    val accessLevel: Int?
)

enum class TGitProjectType {
    SVN, GIT
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitAclConfig(
    @JsonProperty("allow_ips")
    val allowIps: String?,
    @JsonProperty("allow_users")
    val allowUsers: String?,
    @JsonProperty("spec_allow_ips")
    val specAllowIps: String?,
    @JsonProperty("spec_hit_users")
    val specHitUsers: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectMember(
    val username: String?,
    @JsonProperty("access_level")
    val accessLevel: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitSvnAuth(
    @JsonProperty("approver_users")
    val approverUsers: List<TGitSvnAuthUser>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitSvnAuthUser(
    val username: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitGroupInfo(
    val id: Long,
    @JsonProperty("full_path")
    val fullPath: String
)

enum class TGitNamespaceKind(val text: String) {
    GROUP("Group"),
    USER("User")
}
