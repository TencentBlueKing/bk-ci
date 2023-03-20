package com.tencent.devops.turbo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document(collection = "t_turbo_plan_entity")
@CompoundIndexes(
    CompoundIndex(
        name = "project_id_1_open_status_1",
        def = "{'project_id': 1, 'open_status' : 1}",
        background = true
    ),
    CompoundIndex(
        name = "project_id_1_plan_name_1",
        def = "{'project_id': 1, 'plan_name' : 1}",
        background = true
    )
)
data class TTurboPlanEntity(
    @Id
    var id: String? = null,
    // 蓝盾项目id
    @Field("project_id")
    @Indexed(background = true)
    val projectId: String,
    // 方案名称
    @Field("plan_name")
    @Indexed(background = true)
    val planName: String,
    // 蓝盾模板代码
    @Field("engine_code")
    val engineCode: String,
    // 蓝盾模板名称
    @Field("engine_name")
    val engineName: String? = "",
    // 方案说明
    @Field("desc")
    val desc: String? = "",
    // 配置参数值
    @Field("config_param")
    val configParam: Map<String, Any?>?,
    // 编译加速实例数
    @Field("instance_num")
    var instanceNum: Int = 0,
    // 编译加速执行次数
    @Field("execute_count")
    var executeCount: Int = 0,
    // 编译加速预估时间（小时单位）
    @Field("estimate_time")
    var estimateTimeHour: Double = 0.0,
    // 编译加速实际执行时间（小时单位）
    @Field("execute_time")
    var executeTimeHour: Double = 0.0,
    // 白名单
    @Field("white_list")
    val whiteList: String? = "",
    // 开启状态
    @Field("open_status")
    val openStatus: Boolean? = true,
    // 是否置顶
    @Field("top_status")
    val topStatus: String? = "false",
    // 是否迁移
    @Field("migrated")
    val migrated: Boolean? = false,
    // 迁移字段：老项目是否和流水线关联
    @Field("pipeline_related")
    val pipelineRelated: Boolean? = true,
    // 事业群id
    @Field("bg_id")
    val bgId: String? = null,
    // 事业群名字
    @Field("bg_name")
    val bgName: String? = null,
    // 部门id
    @Field("dept_id")
    val deptId: String? = null,
    // 部门名字
    @Field("dept_name")
    val deptName: String? = null,
    // 中心id
    @Field("center_id")
    val centerId: String? = null,
    // 中心名字
    @Field("center_name")
    val centerName: String? = null,

/*    //编译加速方案实例清单
    @DBRef
    @Field("plan_instance_list")
    var planInstanceList : MutableList<TTurboPlanInstanceEntity> = mutableListOf(),*/

    @Field("updated_by")
    var updatedBy: String?,
    @Field("updated_date")
    var updatedDate: LocalDateTime?,
    @Field("created_by")
    @Indexed(background = true)
    var createdBy: String?,
    @Field("created_date")
    var createdDate: LocalDateTime?
)
