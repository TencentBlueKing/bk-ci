package com.tencent.devops.turbo.dto

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * TurboWorkStatsDto jobStatsData反序列化类
 */
data class JobStatsDataDto(

    val id: String,

    val pid: Int,

    @JsonProperty("work_id")
    val workId: String,

    @JsonProperty("task_id")
    val taskId: String,

    @JsonProperty("booster_type")
    val boosterType: String,

    @JsonProperty("remote_worker")
    val remoteWorker: String,

    @JsonProperty("remote_try_times")
    val remoteTryTimes: Int,

    @JsonProperty("remote_work_timeout_sec")
    val remoteWorkTimeoutSec: Int,

    @JsonProperty("remote_work_timeout_setting")
    val remoteWorkTimeoutSetting: Int,

    val success: Boolean,

    @JsonProperty("pre_work_success")
    val preWorkSuccess: Boolean,

    @JsonProperty("remote_work_success")
    val remoteWorkSuccess: Boolean,

    @JsonProperty("post_work_success")
    val postWorkSuccess: Boolean,

    @JsonProperty("final_work_success")
    val finalWorkSuccess: Boolean,

    @JsonProperty("local_work_success")
    val localWorkSuccess: Boolean,

    @JsonProperty("remote_work_timeout")
    val remoteWorkTimeout: Boolean,

    @JsonProperty("remote_work_fatal")
    val remoteWorkFatal: Boolean,

    @JsonProperty("remote_work_timeout_use_suggest")
    val remoteWorkTimeoutUseSuggest: Boolean,

    @JsonProperty("remote_work_often_retry_and_degraded")
    val remoteWorkOftenRetryAndDegraded: Boolean,

    @JsonProperty("origin_args")
    val originArgs: List<String>,

    @JsonProperty("remote_error_message")
    val remoteErrorMessage: String,

    @JsonProperty("enter_time")
    val enterTime: Long,

    @JsonProperty("leave_time")
    val leaveTime: Long,

    @JsonProperty("pre_work_enter_time")
    val preWorkEnterTime: Long,

    @JsonProperty("pre_work_leave_time")
    val preWorkLeaveTime: Long,

    @JsonProperty("pre_work_lock_time")
    val preWorkLockTime: Long,

    @JsonProperty("pre_work_unlock_time")
    val preWorkUnlockTime: Long,

    @JsonProperty("pre_work_start_time")
    val preWorkStartTime: Long,

    @JsonProperty("pre_work_end_time")
    val preWorkEndTime: Long,

    @JsonProperty("post_work_enter_time")
    val postWorkEnterTime: Long,

    @JsonProperty("post_work_leave_time")
    val postWorkLeaveTime: Long,

    @JsonProperty("post_work_lock_time")
    val postWorkLockTime: Long,

    @JsonProperty("post_work_unlock_time")
    val postWorkUnlockTime: Long,

    @JsonProperty("post_work_start_time")
    val postWorkStartTime: Long,

    @JsonProperty("post_work_end_time")
    val postWorkEndTime: Long,

    @JsonProperty("final_work_start_time")
    val finalWorkStartTime: Long,

    @JsonProperty("final_work_end_time")
    val finalWorkEndTime: Long,

    @JsonProperty("remote_work_enter_time")
    val remoteWorkEnterTime: Long,

    @JsonProperty("remote_work_leave_time")
    val remoteWorkLeaveTime: Long,

    @JsonProperty("remote_work_lock_time")
    val remoteWorkLockTime: Long,

    @JsonProperty("remote_work_unlock_time")
    val remoteWorkUnlockTime: Long,

    @JsonProperty("remote_work_start_time")
    val remoteWorkStartTime: Long,

    @JsonProperty("remote_work_end_time")
    val remoteWorkEndTime: Long,

    @JsonProperty("remote_work_pack_start_time")
    val remoteWorkPackStartTime: Long,

    @JsonProperty("remote_work_pack_end_time")
    val remoteWorkPackEndTime: Long,

    @JsonProperty("remote_work_send_start_time")
    val remoteWorkSendStartTime: Long,

    @JsonProperty("remote_work_send_end_time")
    val remoteWorkSendEndTime: Long,

    @JsonProperty("remote_work_pack_common_start_time")
    val remoteWorkPackCommonStartTime: Long,

    @JsonProperty("remote_work_pack_common_end_time")
    val remoteWorkPackCommonEndTime: Long,

    @JsonProperty("remote_work_send_common_start_time")
    val remoteWorkSendCommonStartTime: Long,

    @JsonProperty("remote_work_send_common_end_time")
    val remoteWorkSendCommonEndTime: Long,

    @JsonProperty("remote_work_process_start_time")
    val remoteWorkProcessStartTime: Long,

    @JsonProperty("remote_work_process_end_time")
    val remoteWorkProcessEndTime: Long,

    @JsonProperty("remote_work_receive_start_time")
    val remoteWorkReceiveStartTime: Long,

    @JsonProperty("remote_work_receive_end_time")
    val remoteWorkReceiveEndTime: Long,

    @JsonProperty("remote_work_unpack_start_time")
    val remoteWorkUnpackStartTime: Long,

    @JsonProperty("remote_work_unpack_end_time")
    val remoteWorkUnpackEndTime: Long,

    @JsonProperty("local_work_enter_time")
    val localWorkEnterTime: Long,

    @JsonProperty("local_work_leave_time")
    val localWorkLeaveTime: Long,

    @JsonProperty("local_work_lock_time")
    val localWorkLockTime: Long,

    @JsonProperty("local_work_unlock_time")
    val localWorkUnlockTime: Long,

    @JsonProperty("local_work_start_time")
    val localWorkStartTime: Long,

    @JsonProperty("local_work_end_time")
    val localWorkEndTime: Long
)
