package com.tencent.devops.store.pojo.common

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 原子白名单创建请求报文
 */
@Schema(title = "原子白名单创建请求报文")
data class AtomWhitelistCreateRequest(
    @get:Schema(title = "白名单类型", required = true)
    val whitelistType: String,
    
    @get:Schema(title = "原子代码列表", required = true)
    val atomCodes: List<String>,
    
    @get:Schema(title = "描述", required = false)
    val description: String? = null
)
