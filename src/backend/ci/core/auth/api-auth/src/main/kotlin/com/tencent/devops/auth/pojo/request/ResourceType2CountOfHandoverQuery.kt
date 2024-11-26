package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverQueryChannel
import io.swagger.v3.oas.annotations.media.Schema

data class ResourceType2CountOfHandoverQuery(
    @get:Schema(title = "渠道")
    val queryChannel: HandoverQueryChannel,
    @get:Schema(title = "流程单号")
    val flowNo: String?,
    @get:Schema(title = "项目ID")
    val projectCode: String?,
    @get:Schema(title = "操作的组ID")
    val iamGroupIds: List<Int>?,
    @get:Schema(title = "用户ID")
    val memberId: String?
) {
    fun check() {
        when (queryChannel) {
            HandoverQueryChannel.HANDOVER_APPLICATION -> {
                if (flowNo == null) {
                    throw IllegalArgumentException("flowNo cannot be null!")
                }
            }

            else -> {
                if (projectCode == null || iamGroupIds == null || memberId == null) {
                    throw IllegalArgumentException("projectCode or iamGroupIds or memberId cannot  be null!")
                }
            }
        }
    }
}
