package com.tencent.devops.turbo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document("t_turbo_plan_instance_entity")
@CompoundIndexes(
    CompoundIndex(
        name = "turbo_plan_id_1_client_ip_1",
        def = "{'turbo_plan_id': 1, 'client_ip' : 1}",
        background = true
    ),
    CompoundIndex(
        name = "turbo_plan_id_1_pipeline_id_1_pipeline_element_id_1",
        def = "{'turbo_plan_id': 1, 'pipeline_id' : 1, 'pipeline_element_id' : 1}",
        background = true
    ),
    CompoundIndex(
        name = "project_id_1_pipeline_id_1",
        def = "{'project_id': 1, 'pipeline_id' : 1}",
        background = true
    )
)
data class TTurboPlanInstanceEntity(
    @Id
    var id: String? = null,
    // 关联编译加速方案id
    @Field("turbo_plan_id")
    @Indexed(background = true)
    var turboPlanId: String? = null,
    // 项目id
    @Field("project_id")
    val projectId: String,
    // 流水线id
    @Field("pipeline_id")
    val pipelineId: String? = null,
    // 流水线元素id
    @Field("pipeline_element_id")
    val pipelineElementId: String? = null,
    // 流水线名字
    @Field("pipeline_name")
    val pipelineName: String? = null,
    // 客户端ip
    @Field("client_ip")
    val clientIp: String? = null,
    // 执行次数
    @Field("execute_count")
    var executeCount: Int = 0,
    // 预估非加速执行时间（秒单位）
    @Field("total_estimate_time")
    var totalEstimateTimeSecond: Long = 0L,
    // 加速执行时间（秒单位）
    @Field("total_execute_time")
    var totalExecuteTimeSecond: Long = 0L,
    // 最新开始时间
    @Field("latest_start_time")
    var latestStartTime: LocalDateTime? = null,
    // 最新状态
    @Field("latest_status")
    var latestStatus: String? = null,

    @Field("updated_by")
    var updatedBy: String?,
    @Field("updated_date")
    var updatedDate: LocalDateTime?,
    @Field("created_by")
    var createdBy: String?,
    @Field("created_date")
    var createdDate: LocalDateTime?
)
