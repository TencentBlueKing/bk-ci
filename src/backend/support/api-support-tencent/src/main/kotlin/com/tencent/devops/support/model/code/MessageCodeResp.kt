package com.tencent.devops.support.model.code

import com.tencent.devops.common.api.pojo.MessageCodeDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回码列表信息")
data class MessageCodeResp(
    @ApiModelProperty("总记录数", required = true)
    val count: Long,
    @ApiModelProperty("当前页码值", required = false)
    val page: Int?,
    @ApiModelProperty("每页记录大小", required = false)
    val pageSize: Int?,
    @ApiModelProperty("总页数", required = true)
    val totalPages: Int,
    @ApiModelProperty("数据集合", required = false)
    val records: List<MessageCodeDetail>?
)