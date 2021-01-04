package com.tencent.devops.process.util

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_MANUAL
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_REMOTE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_SERVICE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_TIME
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_WEBHOOK

object BuildMsgUtils {

    fun getDefaultValue(startType: StartType, channelCode: ChannelCode?): String {
        return when (startType) {
            StartType.MANUAL ->
                MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_MANUAL, defaultMessage = "手动触发")
            StartType.TIME_TRIGGER ->
                MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_TIME, defaultMessage = "定时触发")
            StartType.WEB_HOOK ->
                MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_WEBHOOK, defaultMessage = "webhook触发")
            StartType.REMOTE ->
                MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_REMOTE, defaultMessage = "远程触发")
            StartType.SERVICE ->
                if (channelCode != null) {
                    if (channelCode == ChannelCode.BS) {
                        "OpenAPI触发"
                    } else {
                        channelCode.name + "触发"
                    }
                } else {
                    MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_SERVICE, defaultMessage = "服务触发")
                }
            StartType.PIPELINE ->
                MessageCodeUtil.getCodeLanMessage(messageCode = BUILD_MSG_PIPELINE, defaultMessage = "流水线调用触发")
        }
    }

    fun getBuildMsg(buildMsg: String?, startType: StartType, channelCode: ChannelCode?): String {
        return if (buildMsg.isNullOrBlank()) {
            getDefaultValue(startType = startType, channelCode = channelCode)
        } else {
            buildMsg!!
        }
    }
}
