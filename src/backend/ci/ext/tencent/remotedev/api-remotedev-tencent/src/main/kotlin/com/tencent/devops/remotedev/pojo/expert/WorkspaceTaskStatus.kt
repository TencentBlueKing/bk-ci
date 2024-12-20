package com.tencent.devops.remotedev.pojo.expert

import com.fasterxml.jackson.annotation.JsonProperty

// 透传用，全可空防止解析异常

data class WorkspaceTaskStatus(
    val callbackName: String?,
    val commonParams: Map<String, Any>?,
    val commonPayload: String?,
    val createdAt: String?,
    val creator: String?,
    val currentStep: String?,
    @JsonProperty("detailURL")
    val detailUrl: String?,
    val end: String?,
    val executionDuration: String?,
    val executionTime: Long?,
    val lastUpdate: String?,
    val maxExecutionDuration: String?,
    val maxExecutionSeconds: Long?,
    val message: String?,
    val start: String?,
    val status: String?,
    val steps: List<WorkspaceTaskStatusStep>?,
    @JsonProperty("taskID")
    val taskId: String?,
    val taskIndex: String?,
    val taskIndexType: String?,
    val taskName: String?,
    val taskType: String?,
    val updater: String?
)

data class WorkspaceTaskStatusStep(
    val alias: String?,
    val end: String?,
    val eta: String?,
    val executionDuration: String?,
    val executionTime: Long?,
    val executor: String?,
    val lastUpdate: String?,
    val maxExecutionDuration: String?,
    val maxExecutionSeconds: Long?,
    val maxRetries: Long?,
    val message: String?,
    val name: String?,
    val params: Map<String, Any>?,
    val payload: String?,
    val retryCount: Long?,
    val skipOnFailed: Boolean?,
    val start: String?,
    val status: String?
)
