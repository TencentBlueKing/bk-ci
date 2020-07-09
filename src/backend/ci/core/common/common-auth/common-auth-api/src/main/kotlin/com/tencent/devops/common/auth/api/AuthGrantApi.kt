package com.tencent.devops.common.auth.api

interface AuthGrantApi {

    fun grantResourcePermission(
        instanceId: String

    ): String

    fun removeResourcePermission(

    ): Boolean
}