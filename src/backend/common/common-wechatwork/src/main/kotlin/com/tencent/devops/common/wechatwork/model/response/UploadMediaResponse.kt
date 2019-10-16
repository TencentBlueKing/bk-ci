package com.tencent.devops.common.wechatwork.model.response

import com.tencent.devops.common.wechatwork.model.enums.UploadMediaType

data class UploadMediaResponse(
    val errcode: Int,
    val errmsg: String,
    val type: UploadMediaType,
    val media_id: String,
    val created_at: String
)