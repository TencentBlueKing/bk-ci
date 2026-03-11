package com.tencent.devops.remotedev.filter.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode.PERMISSION_DENIED
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import com.tencent.devops.remotedev.pojo.common.AUTH_HEADER_OAUTH2
import com.tencent.devops.remotedev.pojo.common.DEVX_HEADER_CDI_WORKSPACE_NAME
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
@PreMatching
@RequestFilter
class CDIFilter constructor(
    private val client: Client
) : ContainerRequestFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(CDIFilter::class.java)
        private const val CDI_PATH = "/api/external/cdi/"
    }

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build<String/*key*/, Pair<String?/*userId*/, String/*wrong message*/?>>()

    override fun filter(requestContext: ContainerRequestContext) {
        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path
        if (!path.contains(CDI_PATH)) return
        val cdiToken = requestContext.headers.getFirst(AUTH_HEADER_OAUTH2)
        val appIp = requestContext.headers.getFirst(AUTH_HEADER_DEVOPS_STORE_CODE)
        if (cdiToken.isNullOrBlank() || appIp.isNullOrBlank()) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = PERMISSION_DENIED,
                            params = arrayOf("AUTHORIZATION Header cannot be empty"),
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )
                    )
                    .build()
            )
            return
        }
        kotlin.run cache@{
            if (cache.getIfPresent(cdiToken) == null) {
                val (workspaceName, realAppId) = client.get(ServiceSDKResource::class).checkCDIOauth(
                    cdiToken = cdiToken
                ).data ?: run {
                    logger.warn("checkCDIOauth fail|$cdiToken|$appIp")
                    cache.put(cdiToken, null to "AUTHORIZATION verification failed, please check.")
                    return@cache
                }
                if (realAppId != appIp) {
                    logger.warn("wrong appIp|$cdiToken|$appIp")
                    cache.put(cdiToken, null to "UNAUTHORIZED STORE-CODE")
                    return@cache
                }

                // 获取登陆人
                val userId = client.get(ServiceSDKResource::class).getLoginUserId(
                    workspaceName = workspaceName
                ).data ?: "no_login_user"
                cache.put(cdiToken, "$userId@@$workspaceName" to null)
            }
        }
        val (value, message) = checkNotNull(cache.getIfPresent(cdiToken))
        if (message != null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = PERMISSION_DENIED,
                            params = arrayOf(message),
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )
                    )
                    .build()
            )
            return
        }
        requestContext.headers[AUTH_HEADER_USER_ID]?.set(0, null)
        requestContext.headers[DEVX_HEADER_CDI_WORKSPACE_NAME]?.set(0, null)
        if (value != null) {
            val split = value.split("@@")
            if (requestContext.headers[AUTH_HEADER_USER_ID] != null) {
                requestContext.headers[AUTH_HEADER_USER_ID]?.set(0, split[0])
            } else {
                requestContext.headers.add(AUTH_HEADER_USER_ID, split[0])
            }
            if (requestContext.headers[DEVX_HEADER_CDI_WORKSPACE_NAME] != null) {
                requestContext.headers[DEVX_HEADER_CDI_WORKSPACE_NAME]?.set(0, split[1])
            } else {
                requestContext.headers.add(DEVX_HEADER_CDI_WORKSPACE_NAME, split[1])
            }
        }
    }
}
