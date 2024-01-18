package com.tencent.devops.process.pojo.classify

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线组命中情况")
data class PipelineViewHitFilters(
    @get:Schema(title = "条件列表")
    val filters: MutableList<FilterInfo>,
    @get:Schema(title = "条件关系")
    val logic: String
) {
    data class FilterInfo(
        @get:Schema(title = "关键字")
        val key: String,
        @get:Schema(title = "命中列表")
        val hits: MutableList<Hit>
    ) {
        data class Hit(
            @get:Schema(title = "是否命中")
            val hit: Boolean,
            @get:Schema(title = "对应的值")
            val value: String
        )
    }

    companion object {
        val EMPTY = PipelineViewHitFilters(mutableListOf(), "")
    }
}
