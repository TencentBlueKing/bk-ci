package com.tencent.devops.process.engine.pojo.builds

/**
 * 历史条件查询结果（只包含原始数据，不包含IdValue转换）
 */
data class HistoryConditionQueryResult(
    val values: List<String>,
    val totalCount: Long
)