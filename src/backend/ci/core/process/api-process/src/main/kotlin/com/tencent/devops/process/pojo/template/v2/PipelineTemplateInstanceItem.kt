package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceTriggerConfig
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板实例具体类")
data class PipelineTemplateInstanceItem(
    val id: String,
    @get:Schema(title = "实例化任务ID", required = true)
    val baseId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "流水线构建号", required = true)
    val buildNo: BuildNo?,
    @get:Schema(title = "实例化状态", required = true)
    val status: TemplateInstanceStatus,
    @get:Schema(title = "实例化参数", required = true)
    val params: List<BuildFormProperty>?,
    @get:Schema(title = "流水线触发器配置", required = false)
    val triggerConfigs: List<TemplateInstanceTriggerConfig>? = null,
    @get:Schema(title = "覆盖模版字段", required = false)
    val overrideTemplateField: TemplateInstanceField? = null,
    @get:Schema(title = "yaml文件路径", required = true)
    val filePath: String?,
    @get:Schema(title = "实例化错误信息", required = true)
    val errorMessage: String?,
    @get:Schema(title = "重置实例推荐版本为基准值", required = false)
    val resetBuildNo: Boolean? = false,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "修改者", required = true)
    val modifier: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,
    @get:Schema(title = "修改时间", required = true)
    val updateTime: Long
)
