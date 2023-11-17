package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CmdbPagingInfo(
    @ApiModelProperty(value = "起始索引", required = true)
    @JsonProperty("start_index")
    val startIndex: Int?,
    @ApiModelProperty(value = "页面大小", required = true)
    @JsonProperty("page_size")
    val pageSize: Int?,
    @ApiModelProperty(value = "是否返回TotalRows", notes = "1-返回, 0-不返回（默认）", required = true)
    @JsonProperty("return_total_rows")
    val returnTotalRows: Int?
)