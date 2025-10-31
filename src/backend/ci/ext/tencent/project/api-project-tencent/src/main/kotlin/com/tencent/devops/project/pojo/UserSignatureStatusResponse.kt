package com.tencent.devops.project.pojo

data class UserSignatureStatusResponse(
    val userId: String,
    val signed: Boolean,
    val schemeQrcodeUrl: String? = null,
    val qrCodeUrl: String? = null,
    val projectInformation: String? = null
)
