package com.tencent.devops.common.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 语音消息
 */
class VoiceNotifyPost : BaseNotifyPost() {
    @JsonProperty("Receiver")
    var receiver = ""

    @JsonProperty("TaskName")
    var taskName = ""

    @JsonProperty("Content")
    var content = ""

    @JsonProperty("TransferReceiver")
    var transferReceiver = ""

    @JsonProperty("Interval")
    var interval = 0

    @JsonProperty("RecallTimes")
    var recallTimes = 0

    @JsonProperty("TextNotify.EnableWorkwxNotify")
    var workwxNotifyEnabled = false

    @JsonProperty("TextNotify.NotifyTitle")
    var workwxNotifyTitle = ""

    @JsonProperty("TextNotify.NotifyContent")
    var workwxNotifyContent = ""
}
