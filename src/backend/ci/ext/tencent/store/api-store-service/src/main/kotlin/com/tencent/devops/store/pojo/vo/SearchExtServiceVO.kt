package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.ExtServiceItem
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务搜索")
class SearchExtServiceVO(
    @ApiModelProperty("总记录数")
    val count: Int,
    @ApiModelProperty("当前页码值")
    val page: Int?,
    @ApiModelProperty("每页记录大小")
    val pageSize: Int?,
    @ApiModelProperty("数据集合")
    val records: List<ExtServiceItem>
)