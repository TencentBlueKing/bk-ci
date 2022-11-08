package com.tencent.devops.turbo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document(collection = "t_turbo_report_entity")
@CompoundIndexes(
    CompoundIndex(
        name = "engine_code_1_status_1",
        def = "{'engine_code': 1, 'status' : 1}",
        background = true
    )
)
data class TTurboRecordEntity(
    @Id
    var id: String? = null,
    // --------------冗余字段start---------------
    // 蓝盾项目id
    @Field("project_id")
    @Indexed(background = true)
    val projectId: String,
    // 关联编译加速方案id
    @Field("turbo_plan_id")
    @Indexed(background = true)
    var turboPlanId: String? = null,
    // 流水线id
    @Field("pipeline_id")
    @Indexed(background = true)
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
    // --------------冗余字段end---------------
    // 加速模式代码
    @Field("engine_code")
    val engineCode: String,
    // 编译加速方案实例id
    @Field("turbo_plan_instance_id")
    val turboPlanInstanceId: String,
    // 对应build_id
    @Indexed(background = true)
    @Field("build_id")
    val buildId: String?,
    // 流水线构建id
    @Field("devops_build_id")
    val devopsBuildId: String?,
    // 对应tbs的task_id
    @Indexed(background = true)
    @Field("tbs_record_id")
    val tbsRecordId: String?,
    // 原始数据映射
    @Field("raw_data")
    val rawData: Map<String, Any?>,
    // 预估非加速执行时间（秒单位）
    @Field("estimate_time")
    var estimateTimeSecond: Long = 0L,
    // 加速执行时间字符
    @Field("estimate_time_value")
    var estimateTimeValue: String? = "--",
    // 加速执行时间（秒单位）
    @Field("execute_time")
    var executeTimeSecond: Long = 0L,
    // 加速执行时间字符
    @Field("execute_time_value")
    var executeTimeValue: String? = "--",
    // 节省率
    @Field("turbo_ratio")
    var turboRatio: String? = "--",
    // 开始时间
    @Field("start_time")
//    @Indexed(background = true)
    var startTime: LocalDateTime,
    // 完成时间
    @Field("end_time")
    var endTime: LocalDateTime? = null,
    // 状态
    @Field("status")
//    @Indexed(background = true)
    var status: String,

    // 执行编号
    @Field("execute_num")
//    @Indexed(background = true)
    val executeNum: Int? = null,

    @Field("scene")
    var scene: String? = null,

    @Field("updated_by")
    var updatedBy: String,
    @Field("updated_date")
    var updatedDate: LocalDateTime,
    @Field("created_by")
    var createdBy: String,
    @Field("created_date")
    var createdDate: LocalDateTime
)
