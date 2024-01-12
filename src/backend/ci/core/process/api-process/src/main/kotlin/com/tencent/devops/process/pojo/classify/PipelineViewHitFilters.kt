package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线组命中情况")
data class PipelineViewHitFilters(
    @Schema(description = "条件列表")
    val filters: MutableList<FilterInfo>,
    @Schema(description = "条件关系")
    val logic: String
) {
    data class FilterInfo(
        @Schema(description = "关键字")
        val key: String,
        @Schema(description = "命中列表")
        val hits: MutableList<Hit>
    ) {
        data class Hit(
            @Schema(description = "是否命中")
            val hit: Boolean,
            @Schema(description = "对应的值")
            val value: String
        )
    }

    companion object {
        val EMPTY = PipelineViewHitFilters(mutableListOf(), "")
    }
}
