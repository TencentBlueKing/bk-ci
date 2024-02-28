package com.tencent.devops.store.pojo.template

import com.tencent.devops.common.pipeline.container.Stage
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装模板到项目返回报文")
data class InstallProjectTemplateDTO(
    @get:Schema(title = "模版名称", required = true)
    val name: String,
    @get:Schema(title = "模版ID", required = true)
    val templateId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val version: Long,
    @get:Schema(title = "最新版本号", required = true)
    val versionName: String,
    @get:Schema(title = "模板类型", required = true)
    val templateType: String,
    @get:Schema(title = "模板类型描述", required = true)
    val templateTypeDesc: String,
    @get:Schema(title = "应用范畴", required = true)
    val category: List<String?>,
    @get:Schema(title = "模版logo", required = true)
    val logoUrl: String,
    @get:Schema(title = "阶段集合", required = true)
    val stages: List<Stage>,
    @get:Schema(title = "父模板ID", required = true)
    val srcTemplateId: String
)
