package com.tencent.devops.remotedev.filter.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.filter.ApiFilter
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
@PreMatching
@RequestFilter
class ClientVersionFilter constructor(
    private val cacheService: RedisCacheService
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(ClientVersionFilter::class.java)
        private const val BK_CI_CLIENT_VERSION = "BK-CI-CLIENT-VERSION"
        private val MINIMUM_VERSION = listOf(0, 3, 0)
    }

    enum class ApiType(val startContextPath: String, val verify: Boolean) {

        USER("/api/user/", true),
        DESKTOP("/api/desktop/", false),
        EXTERNAL("/api/external/", false),
        REMOTEDEV("/api/remotedev/", false),
        SERVICE("/api/service/", false),
        OP("/api/op/", false),
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
        // 开关
        if (cacheService.get(RedisKeys.REDIS_CLIENT_VERSION_CHECK).toString() == false.toString()) return true
        // 判断是否为合法的路径
        val apiType = ApiType.parseType(path) ?: return false
        // 如果是op的接口访问直接跳过jwt认证
        if (!apiType.verify) return true

        val version = requestContext.headers[BK_CI_CLIENT_VERSION]?.get(0)?.split(".") ?: kotlin.run {
            logger.info(
                "user(${requestContext.headers[AUTH_HEADER_USER_ID]}) request" +
                        " $path not have $BK_CI_CLIENT_VERSION,return error."
            )
            return false
        }
        MINIMUM_VERSION.forEachIndexed { index, s ->
            if (version.lastIndex < index || s > (version[index].toIntOrNull() ?: -1)) {
                logger.info(
                    "user(${requestContext.headers[AUTH_HEADER_USER_ID]}) request" +
                            " $path $BK_CI_CLIENT_VERSION=$version < $MINIMUM_VERSION,return error."
                )
                return false
            }
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verify(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = ErrorCodeEnum.CLIENT_NEED_UPDATED.errorCode,
                            params = arrayOf(cacheService.get(RedisKeys.REDIS_CLIENT_INSTALL_URL).toString()),
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            defaultMessage = ErrorCodeEnum.CLIENT_NEED_UPDATED.formatErrorMessage
                        )
                    )
                    .build()
            )
            return
        }
    }
}
