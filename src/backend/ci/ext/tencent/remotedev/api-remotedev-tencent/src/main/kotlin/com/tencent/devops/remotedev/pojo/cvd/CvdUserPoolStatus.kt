package com.tencent.devops.remotedev.pojo.cvd

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD用户在资源池中的状态")
enum class CvdUserPoolStatus {
    @Schema(description = "未领取")
    UNCLAIMED,
    @Schema(description = "领取中")
    CLAIMING,
    @Schema(description = "领取失败")
    CLAIM_FAILED,
    @Schema(description = "已领取")
    CLAIMED,
    @Schema(description = "正在退回")
    RETURNING,
    @Schema(description = "退回失败")
    RETURN_FAILED
}
