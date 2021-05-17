package com.tencent.bk.codecc.defect.pojo

data class UnionFindNodeInfo<T> @ExperimentalUnsignedTypes constructor(
    var parentIndex: Int,
    var pinpointHash: String?,
    var aggregateDefectInputModel: List<T>?,
    var fuzzyHashInfoModel: FuzzyHashInfoModel?
)