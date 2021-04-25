package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AuthExRegisterApi
import com.tencent.devops.common.auth.pojo.CodeCCAuthServiceCode
import org.springframework.beans.factory.annotation.Autowired

class V3AuthExRegisterApi @Autowired constructor(
    private val authResourceApi: AuthResourceApi
): AuthExRegisterApi {
    override fun registerCodeCCTask(user: String, taskId: String, taskName: String, projectId: String): Boolean {
        authResourceApi.createResource(user, CodeCCAuthServiceCode(), AuthResourceType.CODECC_TASK, projectId, taskId, taskName)
        return true
    }

    override fun deleteCodeCCTask(taskId: String, projectId: String): Boolean {
        return true
    }
}