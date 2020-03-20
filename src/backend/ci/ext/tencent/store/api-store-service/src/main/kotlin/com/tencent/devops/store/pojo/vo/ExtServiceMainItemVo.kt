package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.ExtServiceItem
import io.swagger.annotations.ApiModel

@ApiModel("扩展服务-首页")
data class ExtServiceMainItemVo(
    val key: String,
    val label: String,
    val records: List<ExtServiceItem?>
)