package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.constant.AuthMessageCode.INVALID_HANDOVER_TO
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "一键移出用户出项目")
data class RemoveMemberFromProjectReq(
    @get:Schema(title = "目标对象")
    val targetMember: ResourceMemberInfo,
    @get:Schema(title = "授予人")
    val handoverTo: ResourceMemberInfo?
) {
    fun checkHandoverTo() {
        if (handoverTo != null && handoverTo.id == targetMember.id) {
            throw ErrorCodeException(
                errorCode = INVALID_HANDOVER_TO
            )
        }
    }

    fun isNeedToHandover(): Boolean {
        return handoverTo != null
    }
}
