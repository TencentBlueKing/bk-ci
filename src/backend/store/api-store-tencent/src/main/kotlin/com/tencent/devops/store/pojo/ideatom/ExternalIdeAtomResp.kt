package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE里搜索插件返回报文")
data class ExternalIdeAtomResp(
    @ApiModelProperty("总记录数")
    val count: Int,
    @ApiModelProperty("当前页码值")
    val page: Int?,
    @ApiModelProperty("每页记录大小")
    val pageSize: Int?,
    @ApiModelProperty("数据集合")
    val records: List<ExternalIdeAtomItem?>
)