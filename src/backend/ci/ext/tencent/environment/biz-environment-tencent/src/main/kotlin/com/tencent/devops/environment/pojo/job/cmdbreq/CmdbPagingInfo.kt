package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CmdbPagingInfo(
    @get:Schema(title = "起始索引", required = true)
    @JsonProperty("start_index")
    val startIndex: Int?,
    @get:Schema(title = "页面大小", required = true)
    @JsonProperty("page_size")
    val pageSize: Int?,
    @get:Schema(title = "是否返回TotalRows", description = "1-返回, 0-不返回（默认）", required = true)
    @JsonProperty("return_total_rows")
    val returnTotalRows: Int?
)