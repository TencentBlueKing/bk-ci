package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "流水线组命中情况")
data class PipelineViewHitFilters(
    @Schema(name = "条件列表")
    val filters: MutableList<FilterInfo>,
    @Schema(name = "条件关系")
    val logic: String
) {
    data class FilterInfo(
        @Schema(name = "关键字")
        val key: String,
        @Schema(name = "命中列表")
        val hits: MutableList<Hit>
    ) {
        data class Hit(
            @Schema(name = "是否命中")
            val hit: Boolean,
            @Schema(name = "对应的值")
            val value: String
        )
    }

    companion object {
        val EMPTY = PipelineViewHitFilters(mutableListOf(), "")
    }
}
