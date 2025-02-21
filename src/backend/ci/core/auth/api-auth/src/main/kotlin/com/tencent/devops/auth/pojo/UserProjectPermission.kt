package com.tencent.devops.auth.pojo

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.Objects

@Schema(title = "成员项目权限")
data class UserProjectPermission(
    val memberId: String,
    val projectCode: String,
    val action: String,
    val iamGroupId: Int,
    val expireTime: LocalDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val otherObj = other as UserProjectPermission
        return memberId == otherObj.memberId && projectCode == otherObj.projectCode
            && action == otherObj.action && iamGroupId == otherObj.iamGroupId
            && expireTime.timestampmilli() == otherObj.expireTime.timestampmilli()
    }

    override fun hashCode(): Int {
        return Objects.hash(memberId, projectCode, action, iamGroupId, expireTime.timestampmilli())
    }
}
