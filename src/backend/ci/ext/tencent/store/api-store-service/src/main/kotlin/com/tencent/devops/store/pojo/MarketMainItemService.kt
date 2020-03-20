package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel

@ApiModel("扩展市场-首页标签")
class MarketMainItemService(
    val key: String,
    val bkService: String
)