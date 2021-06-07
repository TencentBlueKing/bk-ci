package com.tencent.bk.codecc.task.pojo

data class CodeCCAccountAuthInfo(
    val userName: String,
    val passWord: String,
    val privateToken: String?,
    override val commitId: String? = null
) : CodeCCAuthInfo("account", commitId)
