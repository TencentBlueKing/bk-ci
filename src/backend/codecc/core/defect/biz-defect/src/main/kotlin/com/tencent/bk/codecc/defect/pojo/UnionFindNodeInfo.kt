package com.tencent.bk.codecc.defect.pojo


data class UnionFindNodeInfo @ExperimentalUnsignedTypes constructor(
    var parentIndex: Int,
    var pinpointHash: String?,
    var aggregateDefectInputModel: List<AggregateDefectInputModel>?,
    var fuzzyHashInfoModel: FuzzyHashInfoModel?
)