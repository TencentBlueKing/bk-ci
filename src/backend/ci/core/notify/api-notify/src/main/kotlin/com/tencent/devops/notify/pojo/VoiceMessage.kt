package com.tencent.devops.notify.pojo

import com.tencent.devops.common.notify.pojo.VoiceNotifyPost
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("语音信息")
open class VoiceMessage : BaseMessage() {
    @ApiModelProperty("接收人(英文ID)，支持多个")
    var receivers: Set<String> = emptySet()

    @ApiModelProperty("任务名称，不超过200字符")
    var taskName: String = ""

    @ApiModelProperty("呼叫内容，建议只传简短的文字内容，详细信息通过企业微信提醒方式发送")
    var content: String = ""

    @ApiModelProperty("转接责任人(英文ID)，单人")
    var transferReceiver: String = ""

    @ApiModelProperty("重呼间隔（秒），默认为0")
    var interval: Int = 0

    @ApiModelProperty("最大重呼次数，默认为0")
    var recallTime: Int = 0

    @ApiModelProperty("企业微信提醒")
    var textNotify: TextNotify = TextNotify.DEFAULT

    @ApiModel("语音信息--企业微信提醒")
    data class TextNotify(
        @ApiModelProperty("是否开启企业微信提醒")
        val enabled: Boolean = false,
        @ApiModelProperty("提醒Title")
        val title: String = "",
        @ApiModelProperty("提醒内容")
        val content: String = ""
    ) {
        companion object {
            val DEFAULT = TextNotify(false, "", "")
        }
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
