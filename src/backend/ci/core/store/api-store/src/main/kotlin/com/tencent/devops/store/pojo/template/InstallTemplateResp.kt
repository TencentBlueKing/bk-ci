package com.tencent.devops.store.pojo.template

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装模板到项目返回报文")
data class InstallTemplateResp(
    @ApiModelProperty("安装结果")
    val result: Boolean?,
    @ApiModelProperty("安装项目模板返回信息")
    val installProjectTemplateDTO: List<InstallProjectTemplateDTO>
)
