package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class FastPushFileRequest(
    val userId: String,
    val fileSources: List<FileSource>,
    val fileTargetPath: String,
    val envSet: EnvSet,
    val account: String,
    val timeout: Long
) {
    data class FileSource(
        val files: List<String>,
        val envSet: EnvSet,
        val account: String
    )
}
