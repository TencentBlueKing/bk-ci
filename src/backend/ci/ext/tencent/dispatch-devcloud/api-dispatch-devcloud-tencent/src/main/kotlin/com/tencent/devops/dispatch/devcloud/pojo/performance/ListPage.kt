package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "分页数据包装模型")
data class ListPage<out T>(
    @Schema(description = "总记录行数", required = true)
    val count: Long,
    @Schema(description = "第几页", required = true)
    val page: Int,
    @Schema(description = "每页多少条", required = true)
    val pageSize: Int,
    @Schema(description = "总共多少页", required = true)
    val totalPages: Int,
    @Schema(description = "数据", required = true)
    val records: List<T>
) {
    constructor(page: Int, pageSize: Int, count: Long, records: List<T>) :
            this(count, page, pageSize, Math.ceil(count * 1.0 / pageSize).toInt(), records)
}
