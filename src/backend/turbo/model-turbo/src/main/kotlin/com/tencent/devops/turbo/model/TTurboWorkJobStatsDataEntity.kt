package com.tencent.devops.turbo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * TurboWorkStatsDto jobStatsData反序列化后的实体类
 */
@Document(collection = "t_turbo_work_job_stats")
data class TTurboWorkJobStatsDataEntity(

    /**
     * 所属TTurboWorkStatsEntity的id
     */
    @Field("work_stats_entity_id")
    var workStatsEntityId: String = "",

    /**
     * 实体类id
     */
    @Id
    var entityId: String? = null,

    @Field("work_id")
    val workId: String = "",

    @Field("tbs_record_id")
    val taskId: String = "",

    /**
     * 同步过来的统计数据id
     */
    @Field("stats_id")
    val id: String = "",

    @Field
    val pid: Int = -1,

    @Field("booster_type")
    val boosterType: String = "",

    @Field("remote_worker")
    val remoteWorker: String? = null,

    @Field("remote_try_times")
    val remoteTryTimes: Int? = null,

    @Field("remote_work_timeout_sec")
    val remoteWorkTimeoutSec: Int? = null,

    @Field("remote_work_timeout_setting")
    val remoteWorkTimeoutSetting: Int? = null,

    val success: Boolean? = null,

    @Field("pre_work_success")
    val preWorkSuccess: Boolean? = null,

    @Field("remote_work_success")
    val remoteWorkSuccess: Boolean? = null,

    @Field("post_work_success")
    val postWorkSuccess: Boolean? = null,

    @Field("final_work_success")
    val finalWorkSuccess: Boolean? = null,

    @Field("local_work_success")
    val localWorkSuccess: Boolean? = null,

    @Field("remote_work_timeout")
    val remoteWorkTimeout: Boolean? = null,

    @Field("remote_work_fatal")
    val remoteWorkFatal: Boolean? = null,

    @Field("remote_work_timeout_use_suggest")
    val remoteWorkTimeoutUseSuggest: Boolean? = null,

    @Field("remote_work_often_retry_and_degraded")
    val remoteWorkOftenRetryAndDegraded: Boolean? = null,

    @Field("origin_args")
    val originArgs: List<String> = emptyList(),

    @Field("remote_error_message")
    val remoteErrorMessage: String = "",

    @Field("enter_time")
    val enterTime: Long = 0L,

    @Field("leave_time")
    val leaveTime: Long = 0L,

    @Field("pre_work_enter_time")
    val preWorkEnterTime: Long = 0L,

    @Field("pre_work_leave_time")
    val preWorkLeaveTime: Long = 0L,

    @Field("pre_work_lock_time")
    val preWorkLockTime: Long = 0L,

    @Field("pre_work_unlock_time")
    val preWorkUnlockTime: Long = 0L,

    @Field("pre_work_start_time")
    val preWorkStartTime: Long = 0L,

    @Field("pre_work_end_time")
    val preWorkEndTime: Long = 0L,

    @Field("post_work_enter_time")
    val postWorkEnterTime: Long = 0L,

    @Field("post_work_leave_time")
    val postWorkLeaveTime: Long = 0L,

    @Field("post_work_lock_time")
    val postWorkLockTime: Long = 0L,

    @Field("post_work_unlock_time")
    val postWorkUnlockTime: Long = 0L,

    @Field("post_work_start_time")
    val postWorkStartTime: Long = 0L,

    @Field("post_work_end_time")
    val postWorkEndTime: Long = 0L,

    @Field("final_work_start_time")
    val finalWorkStartTime: Long = 0L,

    @Field("final_work_end_time")
    val finalWorkEndTime: Long = 0L,

    @Field("remote_work_enter_time")
    val remoteWorkEnterTime: Long = 0L,

    @Field("remote_work_leave_time")
    val remoteWorkLeaveTime: Long = 0L,

    @Field("remote_work_lock_time")
    val remoteWorkLockTime: Long = 0L,

    @Field("remote_work_unlock_time")
    val remoteWorkUnlockTime: Long = 0L,

    @Field("remote_work_start_time")
    val remoteWorkStartTime: Long = 0L,

    @Field("remote_work_end_time")
    val remoteWorkEndTime: Long = 0L,

    @Field("remote_work_pack_start_time")
    val remoteWorkPackStartTime: Long = 0L,

    @Field("remote_work_pack_end_time")
    val remoteWorkPackEndTime: Long = 0L,

    @Field("remote_work_send_start_time")
    val remoteWorkSendStartTime: Long = 0L,

    @Field("remote_work_send_end_time")
    val remoteWorkSendEndTime: Long = 0L,

    @Field("remote_work_pack_common_start_time")
    val remoteWorkPackCommonStartTime: Long = 0L,

    @Field("remote_work_pack_common_end_time")
    val remoteWorkPackCommonEndTime: Long = 0L,

    @Field("remote_work_send_common_start_time")
    val remoteWorkSendCommonStartTime: Long = 0L,

    @Field("remote_work_send_common_end_time")
    val remoteWorkSendCommonEndTime: Long = 0L,

    @Field("remote_work_process_start_time")
    val remoteWorkProcessStartTime: Long = 0L,

    @Field("remote_work_process_end_time")
    val remoteWorkProcessEndTime: Long = 0L,

    @Field("remote_work_receive_start_time")
    val remoteWorkReceiveStartTime: Long = 0L,

    @Field("remote_work_receive_end_time")
    val remoteWorkReceiveEndTime: Long = 0L,

    @Field("remote_work_unpack_start_time")
    val remoteWorkUnpackStartTime: Long = 0L,

    @Field("remote_work_unpack_end_time")
    val remoteWorkUnpackEndTime: Long = 0L,

    @Field("local_work_enter_time")
    val localWorkEnterTime: Long = 0L,

    @Field("local_work_leave_time")
    val localWorkLeaveTime: Long = 0L,

    @Field("local_work_lock_time")
    val localWorkLockTime: Long = 0L,

    @Field("local_work_unlock_time")
    val localWorkUnlockTime: Long = 0L,

    @Field("local_work_start_time")
    val localWorkStartTime: Long = 0L,

    @Field("local_work_end_time")
    val localWorkEndTime: Long = 0L
)
