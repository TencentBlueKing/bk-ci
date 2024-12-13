package com.tencent.devops.remotedev.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 蓝鲸应用相关配置
 */
@Component
class BkConfig @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    // 蓝鲸应用信息
    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    // cc 相关配置
    @Value("\${bkCC.host:}")
    val ccHost: String = ""

    @Value("\${bkCC.userName:}")
    val ccUserName: String = ""

    @Value("\${bkCC.bizId:#{null}}")
    val ccBizId: Int? = null

    // base 相关配置
    @Value("\${bkbase.url:}")
    val baseUrl: String = ""

    @Value("\${bkbase.token:}")
    val baseToken: String = ""

    // apigwRemotedev 配置
    @Value("\${apigw.remoteDevUrl:}")
    val remoteDevUrl: String = ""

    // itsm配置
    @Value("\${bkitsm.host:}")
    val itsmHost: String = ""
    @Value("\${bkitsm.tgitLinkServiceId:#{null}}")
    val tgitLinkServiceId: Int? = null
    @Value("\${bkitsm.recordViewServiceId:#{null}}")
    val recordViewServiceId: Int? = null
    @Value("\${bkitsm.dailyCheckServiceId:#{null}}")
    val dailyCheckServiceId: Int? = null
    @Value("\${bkitsm.userAuthCheckServiceId:#{null}}")
    val userAuthCheckServiceId: Int? = null

    // bkvision配置
    @Value("\${bkvision.url:}")
    val bkvisionUrl: String = ""

    @Value("\${bksops.createTask:}")
    val bksopsCreateTask: String = ""
    @Value("\${bksops.startTask:}")
    val bksopsStartTask: String = ""

    @Value("\${remoteDev.desktopSdkToken:D1oXVCZnVQ9Vu65eXG5R}")
    val desktopSdkToken: String = "D1oXVCZnVQ9Vu65eXG5R"

    // 货币化配置
    @Value("\${bills.pushUrl:}")
    val billsPushUrl: String = ""

    @Value("\${bills.platformKey:}")
    val billsPlatformKey: String = ""

    // bknodeman配置
    @Value("\${bknodeman.host:}")
    val bknodemanHost: String = ""

    @Value("\${bknodeman.userName:}")
    val bknodemanUserName: String = ""

    @Value("\${bknodeman.bizId:#{null}}")
    val bknodemanBizId: Int? = null

    fun headerStr(): String {
        return objectMapper.writeValueAsString(
            mapOf(
                "bk_app_code" to appCode,
                "bk_app_secret" to appSecret,
                "bk_username" to ccUserName
            )
        ).replace("\\s".toRegex(), "")
    }
}
