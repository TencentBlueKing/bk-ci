package com.tencent.devops.environment.pojo

import com.tencent.devops.environment.config.async.AsyncExecuteEventType

interface AsyncExecuteEventData {
    fun toType(): AsyncExecuteEventType
}

data class AsyncInstallImateData(
    val projectId: String,
    val agentId: Long
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_INSTALL_IMATE
}
