package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel

@ApiModel("IDE插件市场-首页")
data class MarketIdeAtomMainItem(
    val key: String,
    val label: String,
    val records: List<MarketIdeAtomItem?>
)