package com.tencent.bkrepo.auth.pojo.user

data class UpdataUserPwdRequest(
    val oldPwd: String,
    val newPwd: String
)
