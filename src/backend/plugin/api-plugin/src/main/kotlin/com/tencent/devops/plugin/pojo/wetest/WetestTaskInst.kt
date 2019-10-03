package com.tencent.devops.plugin.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest测试实例")
data class WetestTaskInst(
    @ApiModelProperty("test id")
    val testId: String,
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("构建号")
    val buildNo: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("包版本")
    val version: String,
    @ApiModelProperty("通过率")
    val passingRate: String,
    @ApiModelProperty("对应的任务设置id")
    val taskId: String,
    @ApiModelProperty("测试类型")
    val testType: String,
    @ApiModelProperty("脚本类型")
    val scriptType: String,
    @ApiModelProperty("是否是同步: 0-异步 1-同步")
    val synchronized: String,
    @ApiModelProperty("待测试包的路径")
    val sourcePath: String,
    @ApiModelProperty("脚本文件路径")
    val scriptPath: String,
    @ApiModelProperty("测试账号文件路径")
    val accountFile: String,
    @ApiModelProperty("仓库类型")
    val sourceType: String,
    @ApiModelProperty("脚本文件仓库类型")
    val scriptSourceType: String,
    @ApiModelProperty("账号名称仓库类型")
    val accountSourceType: String,
    @ApiModelProperty("公有云-0 私有云-1")
    val privateCloud: String,
    @ApiModelProperty("启动用户")
    val startUserId: String,
    @ApiModelProperty("开始时间")
    val beginTime: Long,
    @ApiModelProperty("结束时间")
    val endTime: Long? = null,
    @ApiModelProperty("团队ID")
    val emailGroupId: Long? = null
)