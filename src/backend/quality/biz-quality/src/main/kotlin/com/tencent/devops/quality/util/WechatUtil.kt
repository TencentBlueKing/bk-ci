package com.tencent.devops.quality.util

import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.quality.pojo.RuleCheckSingleResult

object WechatUtil {
    fun makeEndMessage(
        projectName: String,
        pipelineName: String,
        buildNo: String,
        time: String,
        interceptList: List<String>,
        url: String,
        receivers: Set<String>
    ): WechatNotifyMessage {
        val interceptListString = interceptList.joinToString("；")
        val message = WechatNotifyMessage()
        message.addAllReceivers(receivers)
        message.body = "$pipelineName(#$buildNo)被拦截\n" +
                "所属项目：$projectName\n" +
                "拦截时间：$time\n" +
                "拦截指标：$interceptListString\n" +
                "详情链接：$url"
        return message
    }

    fun makeAuditMessage(
        projectName: String,
        pipelineName: String,
        buildNo: String,
        time: String,
        resultList: List<RuleCheckSingleResult>,
        url: String,
        receivers: Set<String>
    ): WechatNotifyMessage {
        val body = StringBuilder()
        body.append("$pipelineName(#$buildNo)被拦截，需要审核\n")
        body.append("所属项目：$projectName\n")
        body.append("拦截时间：$time\n")
        resultList.forEach { result ->
            body.append("拦截规则：${result.ruleName}\n")
            body.append("拦截指标：\n")
            result.messagePairs.forEach {
                body.append(it.first + "\n")
            }
        }
        body.append("审核链接：$url")

        val message = WechatNotifyMessage()
        message.addAllReceivers(receivers)
        message.body = body.toString()
        return message
    }
}