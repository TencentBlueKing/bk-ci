package com.tencent.devops.turbo.model


import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "t_turbo_work_stats")
data class TTurboWorkStatsEntity(

    /**
     * 实体类id
     */
    @Id
    val entityId: String = "",

    @Field("tbs_record_id")
    val taskId: String,

    @Field("project_id")
    @Indexed(background = true)
    val projectId: String,

    val id: Long,

    @Field("job_local_error")
    val jobLocalError: Int,

    @Field("job_local_ok")
    val jobLocalOk: Int,

    @Field("job_remote_error")
    val jobRemoteError: Int,

    @Field("job_remote_ok")
    val jobRemoteOk: Int,

    @Field("registered_time")
    val registeredTime: Long?,

    val scene: String,

    @Field("start_time")
    val startTime: Long,

    @Field("end_time")
    val endTime: Long,

    val success: Boolean,

    @Field("unregistered_time")
    val unregisteredTime: Long? = null,

    @Field("work_id")
    val workId: String
)
