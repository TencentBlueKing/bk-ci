package com.tencent.devops.common.web.aop

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.annotation.IgnoreUserApiPermission
import com.tencent.devops.common.web.service.ServiceUserApiAuthPermissionResource
import com.tencent.devops.common.web.utils.I18nUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.Path

/**
 * 项目访问权限拦截器
 */
@Aspect
class UserApiPermissionAspect constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) {
    private val visitPermissionCache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<Pair<String, String>, Boolean> { (userId, projectId) ->
            runCatching {
                val projectConsulTag = redisOperation.hget(
                    ConsulConstants.PROJECT_TAG_REDIS_KEY,
                    projectId
                )
                bkTag.invokeByTag(tag = projectConsulTag) {
                    client.get(ServiceUserApiAuthPermissionResource::class).checkVisitPermission(
                        userId = userId,
                        projectId = projectId
                    ).data!!
                }
            }.onFailure { logger.warn("check project visit permission error|$userId|$projectId", it) }
                .getOrNull()
        }

    @Around("@within(com.tencent.devops.common.web.RestResource)")
    fun aroundMethod(jp: ProceedingJoinPoint): Any? {
        checkProjectVisitPermission(jp = jp)
        return jp.proceed()
    }

    private fun checkProjectVisitPermission(jp: ProceedingJoinPoint) {
        val methodSignature = jp.signature as MethodSignature
        // 忽略的接口不需要校验
        val ignorePermission = AnnotationUtils.findAnnotation(
            methodSignature.method, IgnoreUserApiPermission::class.java
        )
        if (ignorePermission != null) {
            return
        }

        val pathAnnotation = AnnotationUtils.findAnnotation(
            jp.target::class.java, Path::class.java
        )
        // 当不是user态接口,或者需要忽略的接口,则不校验
        if (pathAnnotation == null ||
            pathAnnotation.value.isBlank() ||
            !pathAnnotation.value.removePrefix("/").startsWith("user")
        ) {
            return
        }

        val userId = I18nUtil.getRequestUserId() ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("userId")
        )

        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = methodSignature.parameterNames
        var projectId: String? = null
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> projectId = parameterValue[index]?.toString()
                "projectCode" -> projectId = parameterValue[index]?.toString()
                else -> Unit
            }
        }
        // 没有项目ID参数,不需要校验
        if (projectId.isNullOrBlank()) {
            return
        }
        // 校验用户态请求,是否有项目的访问权限
        val visitPermission = visitPermissionCache.get(Pair(userId, projectId))
        logger.info(
            "validate user api project visit permission|${methodSignature.declaringType.name}|" +
                    "${methodSignature.name}|$userId|$projectId|$visitPermission"
        )
        if (visitPermission != true) {
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.USER_NOT_HAVE_PROJECT_PERMISSIONS,
                    params = arrayOf(userId, projectId)
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserApiPermissionAspect::class.java)
        private const val CACHE_MAX_SIZE = 50000L
    }
}
