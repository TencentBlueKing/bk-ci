package com.tencent.bkrepo.maven.pojo.response

data class MavenGAVCResponse(
    val result: List<UriResult>
) {
    data class UriResult(
        val uri: String
    )
}
