package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.BKGPT
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.StreamingOutput

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

    fun streamCompletions(data: BKGPT, ticket: String): StreamingOutput {
        return streamCompletions(
            data.apply {
                method = this@BKGPTService.method
                token = this@BKGPTService.token
                appSecret = this@BKGPTService.appSecret
                appCode = this@BKGPTService.appCode
                this.ticket = ticket
            }
        )
    }

    fun streamCompletions(bkChat: BKGPT): StreamingOutput {
        logger.info("start streamCompletions bkgpt $bkChat")
        val request = HttpPost(url)
        request.entity = StringEntity(JsonUtil.toJson(bkChat, false))
        request.addHeader("Content-Type", "application/json")
        val fileStream = StreamingOutput { output ->
            HttpClients.createDefault().execute(request).entity.content.use { stream ->
                val buffer = ByteArray(512)
                var bytes = stream.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    output.flush()
                    bytes = stream.read(buffer)
                }
            }
        }
        return fileStream
    }
}
