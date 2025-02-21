package com.tencent.devops.store.pojo.common

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "商城组件发布者矫正结果")
class StorePublisherCorrectionResult(
    @get:Schema(title = "执行结果")
    var executionResult: Boolean,
    @get:Schema(title = "成功矫正数量")
    var successCount: Int

)

