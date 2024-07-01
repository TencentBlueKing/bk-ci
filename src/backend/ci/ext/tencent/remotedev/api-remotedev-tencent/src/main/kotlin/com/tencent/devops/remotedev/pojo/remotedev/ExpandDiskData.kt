package com.tencent.devops.remotedev.pojo.remotedev

/**
 * 扩展数据盘相关
 * @param uid 环境id
 * @param size 数据盘扩容大小单位Gi
 */
data class ExpandDiskData(
    val uid: String,
    val size: String
)

/**
 * @param valid 扩容是否合法
 * @param message 信息
 */
data class ExpandDiskValidateResp(
    val valid: Boolean,
    val message: String?
)
