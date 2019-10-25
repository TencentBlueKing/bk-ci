package com.tencent.devops.plugin.listener.measure

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.event.pojo.measure.MeasureRequest
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * deng
 * 2019-05-15
 */
@Component
class MeasureListener : Listener<MeasureRequest> {

    override fun execute(event: MeasureRequest) {
        val startEpoch = System.currentTimeMillis()
        try {
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] Start to send the measure listener")
            val request = Request.Builder()
                .url(event.url)
                .post(RequestBody.create(JSON, event.request))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                    val body = response.body()?.string()
                    if (!response.isSuccessful) {
                        logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] " +
                            "Fail to send the measure data - (${event.url}|${response.code()}|${response.message()}|$body)")
                    }
                }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to send the measure data")
        }
    }

    companion object {
        private val JSON = MediaType.parse("application/json;charset=utf-8")
        private val logger = LoggerFactory.getLogger(MeasureListener::class.java)
    }
}