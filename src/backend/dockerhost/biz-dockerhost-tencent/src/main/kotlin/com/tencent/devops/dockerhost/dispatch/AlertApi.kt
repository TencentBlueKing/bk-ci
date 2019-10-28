package com.tencent.devops.dockerhost.dispatch

import com.tencent.devops.common.api.util.OkhttpUtils
import org.slf4j.LoggerFactory

class AlertApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(AlertApi::class.java)

    fun alert(level: String, title: String, message: String) {
        try {
            val path = "/dispatch/api/dockerhost/alert?level=$level&title=$title&message=$message"
            val request = buildPost(path)
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("BuildDockerResourceApi $path fail. $responseContent")
                }
            }
        } catch (e: Throwable) {
            logger.error("Alert failed.")
        }
    }
}