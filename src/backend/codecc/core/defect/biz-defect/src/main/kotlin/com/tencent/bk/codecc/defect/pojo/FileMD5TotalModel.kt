package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class FileMD5TotalModel(
        @JsonProperty("files_list")
        val fileList : List<FileMD5SingleModel>
)