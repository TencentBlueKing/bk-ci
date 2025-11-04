package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "变量引用详情")
data class VarRefDetail(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "变量名称", required = true)
    val varName: String,
    @get:Schema(title = "资源ID，标识变量所属的资源（如流水线、模板等）", required = true)
    val resourceId: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源版本名称", required = true)
    val resourceVersionName: String = "",
    @get:Schema(title = "引用版本号", required = true)
    val referVersion: Int = 1,
    @get:Schema(title = "阶段ID", required = true)
    val stageId: String = "",
    @get:Schema(title = "容器ID", required = false)
    val containerId: String? = null,
    @get:Schema(title = "任务ID", required = false)
    val taskId: String? = null,
    @get:Schema(title = "位置路径，描述变量在模型中的具体位置，如model.stages[0].containers[0].elements[0].customCondition", required = true)
    val positionPath: String,
    @get:Schema(title = "创建者", required = true)
    val creator: String = "system",
    @get:Schema(title = "修改者", required = true)
    val modifier: String = "system"
)