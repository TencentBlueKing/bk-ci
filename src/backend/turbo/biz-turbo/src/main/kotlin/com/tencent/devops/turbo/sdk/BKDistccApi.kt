package com.tencent.devops.turbo.sdk

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import com.tencent.devops.common.api.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@Component
@ConfigurationProperties(prefix = "bkdistcc")
object BKDistccApi {

    private val logger = LoggerFactory.getLogger(BKDistccApi::class.java)

    private var bkdistccHost: String? = null

    private var port: String? = null

    @Value("\${bkdistcc.host:#{null}}")
    fun setBkdistccHost(bkdistccHost: String) {
        BKDistccApi.bkdistccHost = bkdistccHost
    }

    @Value("\${bkdistcc.port:#{null}}")
    fun setPort(port: String) {
        BKDistccApi.port = port
    }

    /**
     * 查询编译加速工具版本清单
     */
    fun queryBkdistccVersion(): String {
        logger.info("queryBkdistccVersion url: [$bkdistccHost:$port]")

        OkhttpUtils.doGet(
            url = "http://$bkdistccHost:$port/api/v1/disttask/resource/version"
        ).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to execute queryBkdistccVersion because of ${response.message()} with" +
                    "response: $responseBody")
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
            }

            return responseBody
        }
    }
}
