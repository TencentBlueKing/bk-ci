package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RepoInfo(
    val projectId: String,
    val name: String,
    val type: String,
    val category: String,
    val public: Boolean,
    val description: String?,
    val configuration: RepoConfig,
    val createdBy: String,
    val createdDate: String,
    val lastModifiedBy: String,
    val lastModifiedDate: String,
    val quota: Int?,
    val used: Int?
)
