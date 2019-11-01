package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class ExecuteTaskRequest(
    val userId: String,
    val steps: List<Step>,
    val globalVars: List<GlobalVar>,

    val taskId: Int,
    val timeout: Long

)
