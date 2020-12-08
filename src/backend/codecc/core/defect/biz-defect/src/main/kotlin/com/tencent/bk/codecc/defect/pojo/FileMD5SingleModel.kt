package com.tencent.bk.codecc.defect.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class FileMD5SingleModel(
        @JsonProperty("filePath")
        val filePath : String,
        val md5 : String,
        val fileRelPath: String?
)