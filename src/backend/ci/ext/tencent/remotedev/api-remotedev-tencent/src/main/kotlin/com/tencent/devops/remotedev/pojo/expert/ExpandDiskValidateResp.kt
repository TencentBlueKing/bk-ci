package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param valid 扩容是否合法
 * @param message 信息
 */
@Schema(title = "磁盘扩容任务详情")
data class ExpandDiskValidateResp(
    @get:Schema(title = "扩容是否合法")
    val valid: Boolean,
    @get:Schema(title = "不合法原因")
    val message: String?
)
