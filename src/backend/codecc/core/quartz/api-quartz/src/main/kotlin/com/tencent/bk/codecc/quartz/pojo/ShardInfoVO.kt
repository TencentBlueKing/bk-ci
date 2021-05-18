package com.tencent.bk.codecc.quartz.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分片信息视图")
class ShardInfoVO(
        @ApiModelProperty("分片序号")
        val shardNum: Int,
        @ApiModelProperty("分片标记")
        val tag: String,
        @ApiModelProperty("节点清单")
        var nodeList: List<NodeInfoVO>
)