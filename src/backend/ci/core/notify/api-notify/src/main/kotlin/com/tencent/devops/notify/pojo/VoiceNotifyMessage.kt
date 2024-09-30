package com.tencent.devops.notify.pojo

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.notify.pojo.VoiceNotifyPost
import com.tencent.devops.notify.constant.NotifyMQ.NOTIFY_VOICE
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "语音信息")
@Event(NOTIFY_VOICE)
open class VoiceNotifyMessage : BaseMessage() {
    @get:Schema(title = "接收人(英文ID)，支持多个")
    var receivers = mutableSetOf<String>()

    @get:Schema(title = "任务名称，不超过200字符")
    var taskName = ""

    @get:Schema(title = "呼叫内容，建议只传简短的文字内容，详细信息通过企业微信提醒方式发送")
    var content = ""

    @get:Schema(title = "转接责任人(英文ID)，单人")
    var transferReceiver = ""

    @get:Schema(title = "重呼间隔（秒），默认为0")
    var interval = 0

    @get:Schema(title = "最大重呼次数，默认为0")
    var recallTime = 0

    @get:Schema(title = "企业微信提醒")
    var textNotify = TextNotify.DEFAULT

    @Schema(title = "语音信息--企业微信提醒")
    data class TextNotify(
        @get:Schema(title = "是否开启企业微信提醒")
        val enabled: Boolean = false,
        @get:Schema(title = "提醒Title")
        val title: String = "",
        @get:Schema(title = "提醒内容")
        val content: String = ""
    ) {
        companion object {
            val DEFAULT = TextNotify(false, "", "")
        }
    }

    fun addAllReceivers(receiverSet: Set<String>) {
        receivers.addAll(receiverSet)
    }

    fun clearReceivers() {
        receivers.clear()
    }

    @Throws(IllegalArgumentException::class)
    fun asPost(): VoiceNotifyPost {
        checkParams()
        val post = VoiceNotifyPost()
        post.receiver = this.receivers.joinToString(",")
        post.taskName = this.taskName
        post.content = this.content
        post.transferReceiver = this.transferReceiver
        post.interval = this.interval
        post.recallTimes = this.recallTime
        post.workwxNotifyEnabled = this.textNotify.enabled
        post.workwxNotifyTitle = this.textNotify.title
        post.workwxNotifyContent = this.textNotify.content

        return post
    }

    private fun checkParams() {
        if (receivers.isEmpty()) {
            throw IllegalArgumentException("receiver can`t not empty")
        }
        if (taskName.length > 200) {
            throw IllegalArgumentException("the length of task name can`t be greater then 200")
        }
    }
}
