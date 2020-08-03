package com.tencent.bk.codecc.task.pojo

data class CodeCCAccountAuthInfo(
    val userName : String,
    val passWord : String
) : CodeCCAuthInfo("account")