package com.tencent.devops.artifactory.service.pojo

data class JFrogDownloadUserCounts(
    val count: Long,
    val records: List<JFrogDownloadUserCount>
)