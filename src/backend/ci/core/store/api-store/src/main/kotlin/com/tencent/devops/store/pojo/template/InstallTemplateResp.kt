package com.tencent.devops.store.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装模板到项目返回报文")
data class InstallTemplateResp(
    @get:Schema(title = "安装结果")
    val result: Boolean?,
    @get:Schema(title = "安装项目模板返回信息")
    val installProjectTemplateDTO: List<InstallProjectTemplateDTO>
)
