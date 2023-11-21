package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudTopoNode(
    @ApiModelProperty(value = "动态topo节点类型", notes = "对应CMDB API中的 bk_obj_id，例如module、set等")
    @JsonProperty("node_type")
    val nodeType: String,
    @ApiModelProperty(value = "动态topo节点ID", notes = "对应CMDB API中的 bk_inst_id")
    val id: Int
)