package com.tencent.devops.common.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import kotlin.math.ceil

@ApiModel("分页数据包装模型")
data class Page<out T>(
    @ApiModelProperty("总记录行数", required = true)
    val count: Long,
    @ApiModelProperty("第几页", required = true)
    val page: Int,
    @ApiModelProperty("每页多少条", required = true)
    val pageSize: Int,
    @ApiModelProperty("总共多少页", required = true)
    val totalPages: Int,
    @ApiModelProperty("数据", required = true)
    val records: List<T>
) {
    constructor(page: Int, pageSize: Int, count: Long, records: List<T>) : this(
        count = count,
        page = page,
        pageSize = pageSize,
        totalPages = ceil(count * 1.0 / pageSize).toInt(),
        records = records
    )
}
