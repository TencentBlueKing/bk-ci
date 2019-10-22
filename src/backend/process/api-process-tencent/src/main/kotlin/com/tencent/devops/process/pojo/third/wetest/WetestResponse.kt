package com.tencent.devops.process.pojo.third.wetest

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 *
 * //{"taskid":49,"ret":0,"msg":""}
 *
 */
@ApiModel("wetest响应信息")
data class WetestResponse(
    @ApiModelProperty("任务id，失败则不返回", required = false)
    @JsonProperty("taskid", required = false, defaultValue = "")
    val taskId: String?,
    @ApiModelProperty("请求结果，0为成功")
    @JsonProperty("ret")
    val ret: String,
    @ApiModelProperty("提示信息")
    @JsonProperty("msg")
    val msg: String
)