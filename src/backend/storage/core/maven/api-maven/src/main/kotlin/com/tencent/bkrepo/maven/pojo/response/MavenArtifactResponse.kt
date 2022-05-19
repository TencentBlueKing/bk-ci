package com.tencent.bkrepo.maven.pojo.response

data class MavenArtifactResponse(
    val projectId: String,
    val repo: String,
    val created: String,
    val createdBy: String,
    val downloadUri: String,
    val mimeType: String,
    val size: String,
    val checksums: Checksums,
    val originalChecksums: OriginalChecksums,
    val uri: String
) {
    data class Checksums(
        val sha1: String?,
        val md5: String?,
        val sha256: String?
    )

    data class OriginalChecksums(
        val sha256: String?
    )
}
