package com.tencent.devops.remotedev.filter.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_REAL_IP
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.filter.ApiFilter
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_IP_LIST_KEY
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class IpFilter constructor(
    private val redisOperation: RedisOperation
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(IpFilter::class.java)
    }

    private val redisCache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Set<String>?> { key -> redisOperation.getSetMembers(key) }

    enum class ApiType(val path: String) {
        BK_GPT("/api/user/remotedev/bkGPT");

        companion object {
            fun parseType(path: String): ApiType? {
                values().forEach { type ->
                    if (path.contains(other = type.path, ignoreCase = true)) {
                        return type
                    }
                }
                return null
            }
        }
    }

    override fun verify(requestContext: ContainerRequestContext): Boolean {
        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path
        // 判断是否为合法的路径
        val apiType = ApiType.parseType(path) ?: return true
        logger.info("requestContext headers|${requestContext.headers["X-DEVOPS-REAL-IP"]}")

        val ip = requestContext.headers[AUTH_HEADER_DEVOPS_REAL_IP]?.get(0) ?: run {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sorry, AUTH_HEADER_DEVOPS_REAL_IP is null.")
                    .build()
            )
            return true
        }
        if (!isIpInWhitelist(ip, redisCache.get(REDIS_IP_LIST_KEY) ?: return true)) {
            logger.info("ip($ip)wants to access the resource(${apiType.path}), but is blocked.")
            return false
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verify(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        MessageCodeUtil.generateResponseDataObject(
                            messageCode = ErrorCodeEnum.DENIAL_OF_SERVICE.errorCode,
                            params = null,
                            data = null,
                            defaultMessage = ErrorCodeEnum.DENIAL_OF_SERVICE.formatErrorMessage
                        )
                    )
                    .build()
            )
            return
        }
    }

    fun isIpInWhitelist(ip: String, whitelist: Set<String>): Boolean {
        val ips = ip.split(".").toTypedArray()
        val ipAddr = (
            ips[0].toLong() shl 24
                or (ips[1].toLong() shl 16)
                or (ips[2].toLong() shl 8) or ips[3].toLong()
            )

        whitelist.forEach { cidr ->
            val splits = cidr.split("/")
            val type = if (splits.size == 2) splits[1].toInt() else 32
            val mask = ((1L shl 32 - type) - 1L).inv()
            val cidrIp = splits[0]
            val cidrIps = cidrIp.split(".").toTypedArray()
            val cidrIpAddr = (
                cidrIps[0].toLong() shl 24
                    or (cidrIps[1].toLong() shl 16)
                    or (cidrIps[2].toLong() shl 8)
                    or cidrIps[3].toLong()
                )
            if (ipAddr and mask == cidrIpAddr and mask) return true
        }
        return false
    }
}
