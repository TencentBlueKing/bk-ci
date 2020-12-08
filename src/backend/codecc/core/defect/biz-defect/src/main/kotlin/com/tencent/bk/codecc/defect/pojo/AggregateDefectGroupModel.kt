package com.tencent.bk.codecc.defect.pojo

data class AggregateDefectGroupModel(
        var count : Int = 0,
        val filePathList : MutableList<String>
)