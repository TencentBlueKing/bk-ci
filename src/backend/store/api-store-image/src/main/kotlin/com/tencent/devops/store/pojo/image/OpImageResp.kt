package com.tencent.devops.store.pojo.image

import com.tencent.devops.store.pojo.image.response.OpImageItem
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("OP-镜像列表")
data class OpImageResp(
    @ApiModelProperty("总记录数")
    val count: Int,
    @ApiModelProperty("当前页码值")
    val page: Int?,
    @ApiModelProperty("每页记录大小")
    val pageSize: Int?,
    @ApiModelProperty("数据集合")
    val records: List<OpImageItem?>
)