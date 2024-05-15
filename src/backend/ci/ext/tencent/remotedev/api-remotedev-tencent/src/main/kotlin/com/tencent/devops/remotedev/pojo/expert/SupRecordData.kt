package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "专家协助工单记录")
data class SupRecordData(
    @get:Schema(title = "工单ID")
    val id: Long,
    @get:Schema(title = "求助时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "求助内容")
    val content: String
)

data class SupRecordDataResp(
    val count: Int,
    val records: List<SupRecordData>
)