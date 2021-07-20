package com.tencent.bk.codecc.task.pojo

class CodeCCTokenAuthInfo(
    val accessToken: String,
    override val commitId: String? = null
) : CodeCCAuthInfo("token", commitId)
