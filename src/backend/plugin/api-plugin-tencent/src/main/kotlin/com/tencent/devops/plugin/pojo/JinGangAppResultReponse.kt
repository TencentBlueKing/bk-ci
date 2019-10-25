package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚扫面任务结果")
data class JinGangAppResultReponse(
    @ApiModelProperty("任务结果Id")
    val id: Long,
    @ApiModelProperty("构建Id")
    val buildId: String,
    @ApiModelProperty("包名称")
    val fileName: String,
    @ApiModelProperty("版本号")
    val version: String,
    @ApiModelProperty("文件MD5")
    val fileMD5: String,
    @ApiModelProperty("任务Id")
    val taskId: Long,
    @ApiModelProperty("扫描结果的html地址")
    val scanUrl: String,
    @ApiModelProperty("上传者")
    val responseuser: String,
    @ApiModelProperty("扫描结果")
    val result: Map<String, Any>
)
