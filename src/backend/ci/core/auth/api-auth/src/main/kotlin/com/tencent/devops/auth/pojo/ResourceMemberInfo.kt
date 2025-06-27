package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.util.Objects

@Schema(title = "成员信息")
data class ResourceMemberInfo(
    @get:Schema(title = "成员id")
    val id: String,
    @get:Schema(title = "成员名称")
    val name: String? = null,
    @get:Schema(title = "成员类型")
    val type: String,
    @get:Schema(title = "是否离职")
    val departed: Boolean? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val otherObj = other as ResourceMemberInfo
        return id == otherObj.id && type == otherObj.type
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type)
    }
}
