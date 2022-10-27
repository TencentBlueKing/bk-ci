package com.tencent.devops.turbo.dto

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * 同步编译加速统计数据
 */
data class TurboWorkStatsDto(

    @JsonProperty("tbs_record_id")
    val tbsRecordId: String,

    val id: Long,

    @JsonProperty("job_local_error")
    val jobLocalError: Int,

    @JsonProperty("job_local_ok")
    val jobLocalOk: Int,

    @JsonProperty("job_remote_error")
    val jobRemoteError: Int,

    @JsonProperty("job_remote_ok")
    val jobRemoteOk: Int,

    @JsonProperty("job_stats")
    val jobStats: String,

    @JsonProperty("job_stats_data")
    val jobStatsData: String,

    @JsonProperty("project_id")
    val projectId: String,

    @JsonProperty("registered_time")
    val registeredTime: Long,

    val scene: String,

    @JsonProperty("start_time")
    val startTime: Long,

    @JsonProperty("end_time")
    val endTime: Long,

    val success: Boolean,

    @JsonProperty("task_id")
    val taskId: String,

    @JsonProperty("unregistered_time")
    val unregisteredTime: Long,

    @JsonProperty("work_id")
    val workId: String
)
