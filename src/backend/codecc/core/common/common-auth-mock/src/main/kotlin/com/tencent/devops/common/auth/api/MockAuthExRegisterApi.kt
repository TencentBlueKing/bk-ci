package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AuthExRegisterApi

class MockAuthExRegisterApi: AuthExRegisterApi {
    override fun registerCodeCCTask(user: String, taskId: String, taskName: String, projectId: String): Boolean {
        return true
    }

    override fun deleteCodeCCTask(taskId: String, projectId: String): Boolean {
        return true
    }
}