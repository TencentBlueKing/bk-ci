package com.tencent.devops.remotedev.pojo

import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 用户通知信息数据模型
 *
 */
@Schema(title = "用户通知信息")
data class UserNotifyInfo(
    @get:Schema(title = "通知ID")
    val id: Long,
    @get:Schema(title = "通知标题")
    val title: String,
    @get:Schema(title = "通知内容")
    val body: String? = null,
    @get:Schema(title = "通知类型")
    val notifyType: RemoteDevNotifyType,
    @get:Schema(title = "操作人姓名")
    val operator: String,
    @get:Schema(title = "操作人姓名CN")
    val operatorCN: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "是否已读")
    val read: Boolean,
    @get:Schema(title = "已读时间")
    val readTime: LocalDateTime? = null,
    @get:Schema(title = "通知分类")
    val category: NotifyCategory
)

/**
 * 通知分类
 */
enum class NotifyCategory {
    SYSTEM,
    BUSINESS,
    OPERATION;

    companion object {
        fun fromValue(value: String?): NotifyCategory {
            return values().firstOrNull { it.name == value } ?: BUSINESS
        }
    }
}
