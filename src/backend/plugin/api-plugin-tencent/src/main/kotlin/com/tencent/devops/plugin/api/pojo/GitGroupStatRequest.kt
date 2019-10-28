package com.tencent.devops.plugin.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂项目组统计数据请求包")
data class GitGroupStatRequest(
    @ApiModelProperty("日期", required = true)
    val statDate: String,
    @ApiModelProperty("项目总数", required = true)
    val projectCount: Int,
    @ApiModelProperty("开源项目总数", required = true)
    val projectCountOpen: Int,
    @ApiModelProperty("项目增量", required = false)
    var projectIncre: Int,
    @ApiModelProperty("开源项目增量", required = false)
    var projectIncreOpen: Int,
    @ApiModelProperty("提交总数", required = true)
    val commitCount: Int,
    @ApiModelProperty("开源项目提交总数", required = true)
    val commitCountOpen: Int,
    @ApiModelProperty("提交增量", required = false)
    var commitIncre: Int,
    @ApiModelProperty("开源项目提交增量", required = false)
    var commitIncreOpen: Int,
    @ApiModelProperty("用户总数", required = true)
    val userCount: Int,
    @ApiModelProperty("开源项目用户总数", required = true)
    val userCountOpen: Int,
    @ApiModelProperty("提交用户增量", required = false)
    var userIncre: Int,
    @ApiModelProperty("开源项目提交用户增量", required = false)
    var userIncreOpen: Int
)