package com.tencent.bk.codecc.defect.pojo

data class FuzzyHashInfoModel @ExperimentalUnsignedTypes constructor(
    val blockSize: ULong?,
    val b1: String?,
    val b1Length: Int?,
    var b1ParArray: MutableMap<Char, ULong>?,
    val b2: String?,
    val b2Length: Int?,
    var b2ParArray: MutableMap<Char, ULong>?,
    val valid: Boolean?
)