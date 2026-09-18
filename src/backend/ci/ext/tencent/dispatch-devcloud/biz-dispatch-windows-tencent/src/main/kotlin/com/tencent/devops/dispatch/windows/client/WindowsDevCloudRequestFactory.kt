package com.tencent.devops.dispatch.windows.client

import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.common.redis.RedisOperation
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class WindowsDevCloudRequestFactory(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WindowsDevCloudRequestFactory::class.java)
        private const val SGW_SWITCH_KEY = "windows:devcloud:sgw:enabled"
    }

    @Value("\${windows.devCloud.appId:}")
    private lateinit var devCloudAppId: String

    @Value("\${windows.devCloud.token:}")
    private lateinit var devCloudToken: String

    @Value("\${windows.devCloud.url:}")
    private lateinit var devCloudUrl: String

    @Value("\${windows.devCloud.sgwUrl:}")
    private lateinit var devCloudSgwUrl: String

    @Value("\${windows.devCloud.smartProxyToken:}")
    private lateinit var smartProxyToken: String

    @Value("\${devopsGateway.idcProxy:}")
    private lateinit var devopsIdcProxyGateway: String

    @Value("\${windows.devCloud.sgwSwitchCacheSeconds:10}")
    private var switchCacheSeconds: Long = 10

    @Volatile
    private var cachedSgwEnabled: Boolean? = null

    @Volatile
    private var cacheUpdateTime: Long = 0

    fun resolveRoute(): WindowsDevCloudRoute {
        if (!isSgwEnabled()) {
            return WindowsDevCloudRoute.LEGACY
        }
        if (devCloudSgwUrl.isBlank()) {
            logger.error("Windows DevCloud SGW switch is enabled but sgwUrl is empty, fallback to legacy route")
            return WindowsDevCloudRoute.LEGACY
        }
        return WindowsDevCloudRoute.SGW
    }

    fun newRequestBuilder(
        apiPath: String,
        creator: String,
        projectId: String,
        pipelineId: String,
        route: WindowsDevCloudRoute
    ): Request.Builder {
        return when (route) {
            WindowsDevCloudRoute.SGW -> Request.Builder()
                .url(joinUrl(devCloudSgwUrl, apiPath))
                .headers(
                    SmartProxyUtil.makeHeaders(
                        appId = devCloudAppId,
                        token = devCloudToken,
                        staffName = creator,
                        proxyToken = smartProxyToken,
                        projectId = projectId,
                        pipelineId = pipelineId
                    ).toHeaders()
                )
            WindowsDevCloudRoute.LEGACY -> {
                val realUrl = joinUrl(devCloudUrl, apiPath)
                Request.Builder()
                    .url(toIdcUrl(realUrl))
                    .headers(
                        SmartProxyUtil.makeIdcProxyHeaders(
                            appId = devCloudAppId,
                            token = devCloudToken,
                            userId = creator
                        ).toHeaders()
                    )
            }
        }
    }

    fun isSgwEnabled(): Boolean {
        val now = System.currentTimeMillis()
        val cachedValue = cachedSgwEnabled
        if (cachedValue != null && now - cacheUpdateTime < switchCacheSeconds * 1000) {
            return cachedValue
        }

        return try {
            val enabled = redisOperation.get(SGW_SWITCH_KEY)?.toBooleanStrictOrNull() ?: false
            updateSwitchCache(enabled, now)
            enabled
        } catch (e: Exception) {
            val fallback = cachedSgwEnabled ?: false
            updateSwitchCache(fallback, now)
            logger.error("Failed to get Windows DevCloud SGW switch, use cached value: $fallback", e)
            fallback
        }
    }

    fun setSgwEnabled(enabled: Boolean, operator: String): Boolean {
        return try {
            redisOperation.set(SGW_SWITCH_KEY, enabled.toString())
            updateSwitchCache(enabled, System.currentTimeMillis())
            logger.info("Operator $operator set Windows DevCloud SGW switch to $enabled")
            true
        } catch (e: Exception) {
            logger.error("Operator $operator failed to set Windows DevCloud SGW switch to $enabled", e)
            false
        }
    }

    private fun updateSwitchCache(enabled: Boolean, updateTime: Long) {
        cachedSgwEnabled = enabled
        cacheUpdateTime = updateTime
    }

    private fun joinUrl(baseUrl: String, apiPath: String): String {
        return "${baseUrl.trimEnd('/')}/${apiPath.trimStart('/')}"
    }

    private fun toIdcUrl(realUrl: String): String {
        return "$devopsIdcProxyGateway/proxy-devnet?url=${URLEncoder.encode(realUrl, "UTF-8")}"
    }
}

enum class WindowsDevCloudRoute {
    LEGACY,
    SGW
}
