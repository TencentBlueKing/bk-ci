package com.tencent.devops.store.pojo.template

import com.tencent.devops.common.pipeline.container.Stage
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "安装模板到项目返回报文")
data class InstallProjectTemplateDTO(
    @Schema(name = "模版名称", required = true)
    val name: String,
    @Schema(name = "模版ID", required = true)
    val templateId: String,
    @Schema(name = "项目ID", required = true)
    val projectId: String,
    @Schema(name = "模板ID", required = true)
    val version: Long,
    @Schema(name = "最新版本号", required = true)
    val versionName: String,
    @Schema(name = "模板类型", required = true)
    val templateType: String,
    @Schema(name = "模板类型描述", required = true)
    val templateTypeDesc: String,
    @Schema(name = "应用范畴", required = true)
    val category: List<String?>,
    @Schema(name = "模版logo", required = true)
    val logoUrl: String,
    @Schema(name = "阶段集合", required = true)
    val stages: List<Stage>,
    @Schema(name = "父模板ID", required = true)
    val srcTemplateId: String
)
