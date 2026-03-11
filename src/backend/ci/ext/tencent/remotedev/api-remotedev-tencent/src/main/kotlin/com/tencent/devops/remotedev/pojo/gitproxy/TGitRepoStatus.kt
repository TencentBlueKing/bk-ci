package com.tencent.devops.remotedev.pojo.gitproxy

enum class TGitRepoStatus {
    TO_BE_MIGRATED,
    AVAILABLE,
    ABNORMAL;

    companion object {
        fun fromStr(str: String): TGitRepoStatus {
            return when (str) {
                TO_BE_MIGRATED.name -> TO_BE_MIGRATED
                AVAILABLE.name -> AVAILABLE
                else -> ABNORMAL
            }
        }
    }
}
