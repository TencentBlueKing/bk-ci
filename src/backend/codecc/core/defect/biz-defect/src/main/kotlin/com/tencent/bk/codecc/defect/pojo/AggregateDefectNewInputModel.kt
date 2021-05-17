package com.tencent.bk.codecc.defect.pojo

data class AggregateDefectNewInputModel<out T>(
    val filePathSet: Set<String>?,
    val relPathSet : Set<String>?,
    val filterPath: Set<String>?,
    val pathList: Set<String>?,
    val defectList: List<T>
)
