package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚扫面任务")
data class JinGangApp(
    @ApiModelProperty("任务Id")
    val id: Long,
    @ApiModelProperty("项目Id")
    val projectId: String,
    @ApiModelProperty("流水线Id")
    val pipelineId: String,
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("构建Id")
    val buildId: String,
    @ApiModelProperty("构建号")
    val buildNo: Int,
    @ApiModelProperty("版本号")
    val version: String,
    @ApiModelProperty("包名称")
    val fileName: String,
    @ApiModelProperty("文件MD5")
    val fileMD5: String,
    @ApiModelProperty("文件大小(Byte)")
    val fileSize: Long,
    @ApiModelProperty("开始时间")
    val createTime: Long,
    @ApiModelProperty("更新时间")
    val updateTime: Long,
    @ApiModelProperty("执行人")
    val creator: String,
    @ApiModelProperty("状态(成功;失败;扫描中)")
    val status: String,
    @ApiModelProperty("类型(android;ios;其他)")
    val type: String

)
