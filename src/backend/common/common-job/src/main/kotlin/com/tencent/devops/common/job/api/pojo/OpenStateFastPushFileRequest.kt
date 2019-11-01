package com.tencent.devops.common.job.api.pojo

data class OpenStateFastPushFileRequest(
    val userId: String,
    val fileSources: List<FileSource>,
    val fileTargetPath: String,
    val envSet: EnvSet,
    val account: String,
    val timeout: Long,
    val appId: Int,
    val openState: String
) {
    data class FileSource(
        val files: List<String>,
        val envSet: EnvSet,
        val account: String
    )
}
