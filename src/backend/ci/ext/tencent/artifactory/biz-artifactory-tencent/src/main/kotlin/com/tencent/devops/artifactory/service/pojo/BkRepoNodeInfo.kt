package com.tencent.devops.artifactory.service.pojo

data class BkRepoNodeInfo(
    var createdBy: String,
    var createdDate: Long,
    var lastModifiedBy: String,
    var lastModifiedDate: Long,
    var folder: Boolean,
    var path: String,
    var name: String,
    var fullPath: String,
    var size: Long,
    var sha256: String?,
    var metadata: Map<String, String>,
    var projectId: String,
    var repoName: String
)