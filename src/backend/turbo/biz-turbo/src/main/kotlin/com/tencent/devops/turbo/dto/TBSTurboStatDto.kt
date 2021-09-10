package com.tencent.devops.turbo.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 查询tbs统计信息
 */
data class TBSTurboStatDto(
    @JsonProperty("task_id")
    val taskId: String,
    @JsonProperty("project_id")
    val projectId: String,
    val scene: String,
    @JsonProperty("registered_time")
    val registeredTime: Long,
    @JsonProperty("unregistered_time")
    val unregisteredTime: Long,
    @JsonProperty("start_time")
    val startTime: Long,
    @JsonProperty("end_time")
    val endTime: Long,
    @JsonProperty("job_remote_ok")
    val jobRemoteOk: Long,
    @JsonProperty("job_remote_error")
    val jobRemoteError: Long,
    @JsonProperty("job_local_ok")
    val jobLocalOk: Long,
    @JsonProperty("job_local_error")
    val jobLocalError: Long,
    @JsonProperty("job_stats")
    val jobStats: String
)
