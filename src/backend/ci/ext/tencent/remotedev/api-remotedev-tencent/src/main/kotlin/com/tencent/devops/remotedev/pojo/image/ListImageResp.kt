package com.tencent.devops.remotedev.pojo.image

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListVmImagesResp(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: List<StandardVmImage>?
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardVmImage(
    val updateAt: String?,
    val cosFile: String?,
    val sourceType: String?,
    val isStandard: Boolean
)
