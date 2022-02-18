package com.tencent.devops.notify.wework.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class WeworkConfiguration {

    @Value("\${wework.corpId:}")
    lateinit var corpId: String

    @Value("\${wework.corpSecret:}")
    lateinit var corpSecret: String

    @Value("\${wework.apiUrl:https://qyapi.weixin.qq.com}")
    lateinit var apiUrl: String

    @Value("\${wework.agentId:}")
    lateinit var agentId: String

    @Value("\${wework.tempDirectory:}")
    lateinit var tempDirectory: String

    /**
     *  表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
     */
    @Value("\${wework.safe:#{null}}")
    val safe: String? = null

    /**
     * 表示是否开启重复消息检查，0表示否，1表示是，默认0
     */
    @Value("\${wework.enableDuplicateCheck:#{null}}")
    val enableDuplicateCheck: String? = null

    /**
     *  表示是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
     */
    @Value("\${wework.duplicateCheckInterval:#{null}}")
    val duplicateCheckInterval: String? = null

    /**
     *  表示是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
     */
    @Value("\${wework.enableIdTrans:#{null}}")
    val enableIdTrans: String? = null
}
