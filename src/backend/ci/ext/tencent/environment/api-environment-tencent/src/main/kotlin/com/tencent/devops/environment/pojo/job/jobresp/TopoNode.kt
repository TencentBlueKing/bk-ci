package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class TopoNode(
    @get:Schema(title = "动态topo节点类型", description = "对应CMDB API中的 bk_obj_id，例如module、set等")
    val nodeType: String,
    @get:Schema(title = "动态topo节点ID", description = "对应CMDB API中的 bk_inst_id")
    val id: Int
)