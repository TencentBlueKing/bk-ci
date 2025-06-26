package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverQueryChannel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接详细查询请求体")
data class HandoverDetailsQueryReq(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "组/授权资源关联的资源类型")
    val resourceType: String,
    @get:Schema(title = "流程单号")
    val flowNo: String?,
    @get:Schema(title = "交接预览请求条件")
    val previewConditionReq: GroupMemberCommonConditionReq?,
    @get:Schema(title = "渠道")
    val queryChannel: HandoverQueryChannel,
    @get:Schema(title = "第几页")
    val page: Int,
    @get:Schema(title = "每页大小")
    val pageSize: Int
) {
    fun check() {
        when (queryChannel) {
            HandoverQueryChannel.HANDOVER_APPLICATION -> {
                if (flowNo == null) {
                    throw IllegalArgumentException("flowNo cannot be null!")
                }
            }

            else -> {
                if (previewConditionReq == null) {
                    throw IllegalArgumentException("previewConditionReq can not be null!")
                }
            }
        }
    }
}
