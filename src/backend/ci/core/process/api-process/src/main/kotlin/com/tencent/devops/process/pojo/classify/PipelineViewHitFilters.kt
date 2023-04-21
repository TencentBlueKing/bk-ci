package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线组命中情况")
data class PipelineViewHitFilters(
    @ApiModelProperty("条件列表")
    val filters: MutableList<FilterInfo>,
    @ApiModelProperty("条件关系")
    val logic: String
) {
    data class FilterInfo(
        @ApiModelProperty("关键字")
        val key: String,
        @ApiModelProperty("命中列表")
        val hits: MutableList<Hit>
    ) {
        data class Hit(
            @ApiModelProperty("是否命中")
            val hit: Boolean,
            @ApiModelProperty("对应的值")
            val value: String
        )
    }

    companion object {
        val EMPTY = PipelineViewHitFilters(mutableListOf(), "")
    }
}
