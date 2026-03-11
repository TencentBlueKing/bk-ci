package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "分页数据包装模型")
data class ListPage<out T>(
    @get:Schema(title = "总记录行数", required = true)
    val count: Long,
    @get:Schema(title = "第几页", required = true)
    val page: Int,
    @get:Schema(title = "每页多少条", required = true)
    val pageSize: Int,
    @get:Schema(title = "总共多少页", required = true)
    val totalPages: Int,
    @get:Schema(title = "数据", required = true)
    val records: List<T>
) {
    constructor(page: Int, pageSize: Int, count: Long, records: List<T>) :
            this(count, page, pageSize, Math.ceil(count * 1.0 / pageSize).toInt(), records)
}
