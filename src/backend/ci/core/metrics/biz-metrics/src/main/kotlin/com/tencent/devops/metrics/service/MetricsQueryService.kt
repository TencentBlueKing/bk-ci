package com.tencent.devops.metrics.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.metrics.config.MetricsUserConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MetricsQueryService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${metrics.monitor.url:}")
    val monitorUrl: String = ""

    @Value("\${metrics.monitor.appCode:}")
    val monitorAppCode: String = ""

    @Value("\${metrics.monitor.appSecret:}")
    val monitorAppSecret: String = ""

    @Value("\${metrics.monitor.bizId:}")
    val monitorBizId: String = ""
    
    companion object {
        private val logger = LoggerFactory.getLogger(MetricsQueryService::class.java)
    }
    
    /**
     * 查询指标数据
     * @param requestParams 请求参数（不包含bk_biz_id）
     * @return 监控平台返回的数据
     */
    fun queryMetrics(requestParams: Map<String, Any>): Map<String, Any> {
        if (monitorUrl.isEmpty()) {
            return emptyMap()
        }
        // 构建完整的请求参数，添加bk_biz_id
        val fullParams = requestParams.toMutableMap()
        fullParams["bk_biz_id"] = monitorBizId
        
        // 构建请求体
        val requestBody = JsonUtil.toJson(fullParams, false)
        
        // 构建认证头
        val authHeader = JsonUtil.toJson(
            mapOf(
                "bk_app_code" to monitorAppCode,
                "bk_app_secret" to monitorAppSecret
            ),
            false
        )
        
        logger.info("Query metrics with params: $requestBody")
        
        // 发送HTTP请求
        val request = Request.Builder()
            .url(monitorUrl)
            .header("X-Bkapi-Authorization", authHeader)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body?.string() ?: ""
            
            if (!response.isSuccessful) {
                logger.error("Query metrics failed: code=${response.code}, body=$responseBody")
                throw RuntimeException("查询指标数据失败: ${response.code}")
            }
            
            logger.info("Query metrics success: $responseBody")
            
            // 解析响应
            return try {
                objectMapper.readValue(responseBody, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                logger.error("Parse response failed", e)
                throw RuntimeException("解析响应数据失败: ${e.message}")
            }
        }
    }
}
