package com.tencent.devops.common.auth.api

interface AuthUserAndDeptApi {
    /**
     * 检查用户是否离职
     * */
    fun checkUserDeparted(name: String): Boolean
}
