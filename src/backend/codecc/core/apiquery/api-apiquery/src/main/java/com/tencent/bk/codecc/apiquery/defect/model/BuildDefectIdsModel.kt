package com.tencent.bk.codecc.apiquery.defect.model

import com.fasterxml.jackson.annotation.JsonProperty

data class BuildDefectIdsModel(
    @JsonProperty("file_defect_ids")
    val fileDefectIds : String
)
