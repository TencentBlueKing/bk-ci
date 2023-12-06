package com.tencent.devops.store.pojo.template

import com.tencent.devops.common.pipeline.container.Stage
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装模板到项目返回报文")
data class InstallProjectTemplateDTO(
    @ApiModelProperty("模版名称", required = true)
    val name: String,
    @ApiModelProperty("模版ID", required = true)
    val templateId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("当前模板版本ID", required = true)
    val version: Long,
    @ApiModelProperty("父模板版本ID", required = true)
    val srcTemplateVersion: Long,
    @ApiModelProperty("最新版本号", required = true)
    val versionName: String,
    @ApiModelProperty("模板类型", required = true)
    val templateType: String,
    @ApiModelProperty("模板类型描述", required = true)
    val templateTypeDesc: String,
    @ApiModelProperty("应用范畴", required = true)
    val category: List<String?>,
    @ApiModelProperty("模版logo", required = true)
    val logoUrl: String,
    @ApiModelProperty("阶段集合", required = true)
    val stages: List<Stage>,
    @ApiModelProperty("父模板ID", required = true)
    val srcTemplateId: String
)
