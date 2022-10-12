package com.tencent.devops.turbo.sdk

import com.tencent.devops.common.api.util.OkhttpUtil
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
        return OkhttpUtil.doGet(url = "http://$bkdistccHost:$port/api/v1/disttask/resource/version")
    }
}
