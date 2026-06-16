package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateOpenClawData(
    val name: String,
    val ip: String?,
    val projectId: String,
    val params: CreateOpenClawDataParams
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateOpenClawDataParams(
    val envs: List<Map<String, String>>,
    val userName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateOpenClawDataResp(
    val taskId: String,
    val workspaceName: String,
    val ip: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskStatusResp(
    val status: String
)