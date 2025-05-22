package com.tencent.devops.store.pojo.common.version

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal


@Schema(title = "商店组件-版本大小详情")
data class StoreVersionSizeInfo (
    @Schema(description = "组件Code")
    val storeCode: String,
    @Schema(description = "组件类型")
    val storeType: String,
    @Schema(description = "版本号")
    val version: String,
    @Schema(description = "组件包大小")
    val packageSize: BigDecimal?,
    @Schema(description = "单位")
    val unit : String,
)