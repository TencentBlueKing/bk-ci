package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店组件包信息")
data class StorePackageInfoReq(
    @get:Schema(title = "组件类型")
    val storeType: StoreTypeEnum,
    @get:Schema(title = "操作系统名称")
    val osName: String? = "",
    @get:Schema(title = "架构")
    val arch: String? = "",
    @get:Schema(title = "包大小")
    val size: Long
)
