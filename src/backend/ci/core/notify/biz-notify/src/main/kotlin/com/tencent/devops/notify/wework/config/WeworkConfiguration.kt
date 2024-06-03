package com.tencent.devops.notify.wework.config

data class WeworkConfiguration(
    val corpId: String,
    val corpSecret: String,
    val apiUrl: String,
    val agentId: String,
    val tempDirectory: String,
    /**
     *  表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
     */
    val safe: String? = null,
    /**
     * 表示是否开启重复消息检查，0表示否，1表示是，默认0
     */
    val enableDuplicateCheck: String? = null,
    /**
     *  表示是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
     */
    val duplicateCheckInterval: String? = null,
    /**
     *  表示是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
     */
    val enableIdTrans: String? = null
)
