package com.tencent.devops.remotedev.pojo.record

data class WorkspaceLiveResp(
    val url: String
)

enum class WorkspaceLiveResolution(val value: String) {
    R480P("480p"),
    R720P("720p"),
    R1080P("1080p"),
    ;

    companion object {
        fun fromStr(str: String?) = when (str) {
            R480P.name -> R480P
            R720P.name -> R720P
            R1080P.name -> R1080P
            else -> null
        }
    }
}