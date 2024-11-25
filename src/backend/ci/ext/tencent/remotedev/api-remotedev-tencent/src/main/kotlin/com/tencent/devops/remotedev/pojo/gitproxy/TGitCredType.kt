package com.tencent.devops.remotedev.pojo.gitproxy

enum class TGitCredType {
    OAUTH_USER,
    CRED_ACCESS_TOKEN_ID;

    companion object {
        fun fromStringDefault(str: String?): TGitCredType {
            return when (str) {
                OAUTH_USER.name -> OAUTH_USER
                CRED_ACCESS_TOKEN_ID.name -> CRED_ACCESS_TOKEN_ID
                else -> OAUTH_USER
            }
        }
    }
}