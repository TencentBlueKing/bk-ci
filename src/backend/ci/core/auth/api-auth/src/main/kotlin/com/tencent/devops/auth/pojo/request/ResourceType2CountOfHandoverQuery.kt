package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.HandoverQueryChannel
import io.swagger.v3.oas.annotations.media.Schema

data class ResourceType2CountOfHandoverQuery(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "渠道")
    val queryChannel: HandoverQueryChannel,
    @get:Schema(title = "流程单号")
    val flowNo: String?,
    @get:Schema(title = "交接预览请求条件")
    val previewConditionReq: GroupMemberCommonConditionReq?,
    @get:Schema(title = "批量操作动作")
    val batchOperateType: BatchOperateType?
) {
    fun check() {
        when (queryChannel) {
            HandoverQueryChannel.HANDOVER_APPLICATION -> {
                if (flowNo == null) {
                    throw IllegalArgumentException("flowNo cannot be null!")
                }
            }

            else -> {
                if (previewConditionReq == null || batchOperateType == null) {
                    throw IllegalArgumentException("previewConditionReq or batchOperateType can not be null!")
                }
            }
        }
    }
}
