package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.BKGPT
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Scanner

@Service
class BKGPTService {

    @Value("\${bkGPT.bkdata_authentication_method:}")
    val method = ""

    @Value("\${bkGPT.bkdata_data_token:}")
    val token = ""

    @Value("\${bkGPT.bk_app_secret:}")
    val appSecret = ""

    @Value("\${bkGPT.bk_app_code:}")
    val appCode = ""

    @Value("\${bkGPT.url:}")
    val url = ""

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    private val separator = System.getProperty("line.separator")

    fun streamCompletions(data: BKGPT, ticket: String, out: ChunkedOutput<String>) {
        streamCompletions(
            data.apply {
                method = this@BKGPTService.method
                token = this@BKGPTService.token
                appSecret = this@BKGPTService.appSecret
                appCode = this@BKGPTService.appCode
                this.ticket = ticket
            }, out
        )
    }

    fun streamCompletions(bkChat: BKGPT, out: ChunkedOutput<String>) {
        logger.info("start streamCompletions bkgpt $bkChat")
        val request = HttpPost(url)
        request.entity = StringEntity(JsonUtil.toJson(bkChat, false))
        request.addHeader("Content-Type", "application/json")
        HttpClients.createDefault().execute(request).entity.content.use { stream ->
            val scanner = Scanner(stream, "UTF-8")
            while (scanner.hasNextLine()) {
                val value = scanner.nextLine()
                if (!value.isNullOrBlank()) {
                    logger.info("Event: $value")
                    out.write(value + separator)
                }
            }
        }
    }
}
