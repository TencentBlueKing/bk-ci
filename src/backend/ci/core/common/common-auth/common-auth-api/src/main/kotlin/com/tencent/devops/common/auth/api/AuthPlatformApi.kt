package com.tencent.devops.common.auth.api

interface AuthPlatformApi {
    /**
     * 校验用户是否有平台管理的权限
     * @param user 用户ID
     */
    fun validateUserPlatformPermission(user: String): Boolean
}
