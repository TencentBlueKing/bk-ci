package com.tencent.devops.remotedev.filter.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.filter.ApiFilter
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_WHITE_LIST_KEY
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class WhitelistApiFilter constructor(
    private val cacheService: RedisCacheService,
    private val client: Client
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(WhitelistApiFilter::class.java)
    }

    enum class ApiType(val startContextPath: String, val verify: Boolean) {

        USER("/api/user/", true),
        DESKTOP("/api/desktop/", true),
        EXTERNAL("/api/external/", false),
        REMOTEDEV("/api/remotedev/", false),
        SERVICE("/api/service/", false),
        OP("/api/op/", true),
        SWAGGER("/api/swagger.json", false);

        companion object {
            fun parseType(path: String): ApiType? {
                values().forEach { type ->
                    if (path.contains(other = type.startContextPath, ignoreCase = true)) {
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
        val apiType = ApiType.parseType(path) ?: return false
        // 如果是op的接口访问直接跳过jwt认证
        if (!apiType.verify) return true

        val userId = requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.get(0) ?: run {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sorry, X-DEVOPS-UID is null.")
                    .build()
            )
            return true
        }
        // 获取用户的信息，根据 bg 名称加白,先判断人名是否在白名单，不在的话再判断bg名称是否。
        val whiteList = cacheService.getSetMembers(REDIS_WHITE_LIST_KEY) ?: emptySet()
        if (userId !in whiteList && runCatching { client.get(ServiceTxUserResource::class).get(userId) }
                .onFailure { logger.warn("get $userId info error|${it.message}") }
                .getOrNull()?.data?.bgName !in whiteList) {
            logger.info("user($userId)wants to access the resource($path), but is blocked.")
            return false
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verify(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = ErrorCodeEnum.DENIAL_OF_SERVICE.errorCode,
                            params = null,
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )
                    )
                    .build()
            )
            return
        }
    }
}
