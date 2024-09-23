package com.tencent.devops.remotedev.pojo.expert

/**
 * @param valid 扩容是否合法
 * @param message 信息
 */
data class ExpandDiskValidateResp(
    val valid: Boolean,
    val message: String?
)
