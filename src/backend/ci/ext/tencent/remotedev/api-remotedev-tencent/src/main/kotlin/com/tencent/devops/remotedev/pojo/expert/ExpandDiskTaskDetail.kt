package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "磁盘扩容任务详情")
data class ExpandDiskTaskDetail(
    @get:Schema(title = "扩容大小")
    val expandSize: String,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "操作时间")
    val operateDate: LocalDateTime,
    @get:Schema(title = "状态: RUNNING|SUCCEEDED|FAILED|UNKNOW")
    val status: String,
    @get:Schema(title = "操作完成时间")
    val completeDate: LocalDateTime?
)
