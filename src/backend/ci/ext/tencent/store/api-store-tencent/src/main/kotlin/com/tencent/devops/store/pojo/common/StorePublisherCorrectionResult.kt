package com.tencent.devops.store.pojo.common

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "商城组件发布者矫正结果")
class StorePublisherCorrectionResult(
    @get:Schema(title = "是否进行数据矫正")
    var executionResult: Boolean = false,

    @get:Schema(title = "成功执行数量")
    var successExecutionCount: Int = 0,


    @get:Schema(title = "成功矫正数量")
    var successUpdateCount: Int = 0,


    //总共矫正数量
    @get:Schema(title = "执行总数量")
    var totalExecutionCount: Int = 0

)

