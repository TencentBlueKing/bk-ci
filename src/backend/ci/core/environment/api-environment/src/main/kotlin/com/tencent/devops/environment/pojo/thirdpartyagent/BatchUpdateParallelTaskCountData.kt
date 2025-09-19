package com.tencent.devops.environment.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量修改第三方节点并发数")
data class BatchUpdateParallelTaskCountData(
    @get:Schema(title = "Node Hash ID列表", required = true)
    val nodeHashIds: List<String>,
    @get:Schema(title = "最大并发数", required = false)
    val parallelTaskCount: Int?,
    @get:Schema(title = "Docker最大并发数", required = false)
    val dockerParallelTaskCount: Int?
)