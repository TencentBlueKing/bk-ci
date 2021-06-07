package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonIgnore

data class AggregateDefectInputModel(
        val id : String,
        val checkerName : String,
        val pinpointHash : String,
        val filePath : String,
        @JsonIgnore
        val relPath : String?
)