package com.tencent.devops.turbo.model

import com.tencent.devops.turbo.model.pojo.EngineSceneEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDate
import java.time.LocalDateTime

@Document(collection = "t_turbo_day_summary_entity")
@CompoundIndexes(
    CompoundIndex(
        name = "project_id_1_summary_day_1",
        def = "{'project_id': 1, 'summary_day' : 1}",
        background = true
    )
)
data class TTurboDaySummaryEntity(
    @Id
    var id: String? = null,
    // 蓝盾项目id
    @Field("project_id")
    var projectId: String,
    // 预估时间(小时单位)
    @Field("estimate_time")
    var estimateTime: Double?,
    // 执行时间（小时单位）
    @Field("execute_time")
    var executeTime: Double?,
    // 执行次数
    @Field("execute_count")
    var executeCount: Int?,
    // 统计日期
    @Field("summary_day")
    var summaryDay: LocalDate,
    // 加速场景分别加速次数 enum EnumEngineScene
    @Field("engine_scene_list")
    var engineSceneList: List<EngineSceneEntity>,

    @Field("updated_by")
    var updatedBy: String,
    @Field("updated_date")
    var updatedDate: LocalDateTime,
    @Field("created_by")
    var createdBy: String,
    @Field("created_date")
    var createdDate: LocalDateTime
)
