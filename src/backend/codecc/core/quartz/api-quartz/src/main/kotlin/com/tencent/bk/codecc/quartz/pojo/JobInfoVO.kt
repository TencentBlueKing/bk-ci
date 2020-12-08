package com.tencent.bk.codecc.quartz.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行job信息")
data class JobInfoVO(
        @ApiModelProperty("类源码下载地址")
        val classUrl: String,
        @ApiModelProperty("类名字")
        val className: String,
        @ApiModelProperty("job名字")
        val jobName: String,
        @ApiModelProperty("触发器名字")
        val triggerName: String,
        @ApiModelProperty("定时表达式")
        val cronExpression: String,
        @ApiModelProperty("job入参")
        val jobParam: MutableMap<String, Any>?,
        @ApiModelProperty("分片tag名")
        val shardTag: String
)