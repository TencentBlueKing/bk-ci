package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.RoutingMode

/**
 * 批量设置项目模式请求
 */
data class BatchSetProjectModesRequest(
    val projectCodes: List<String>,
    val mode: RoutingMode
)
