package com.tencent.devops.auth.aspect

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class BkProjectMemberCheckAspect(
    private val permissionProjectService: PermissionProjectService
) {
    @Pointcut(
        "@annotation(" +
            "com.tencent.devops.common.auth.api.BkProjectMemberCheck)"
    )
    fun pointCut() = Unit

    @Before("pointCut()")
    fun checkProjectMember(jp: JoinPoint) {
        val parameterValue = jp.args
        val parameterNames =
            (jp.signature as MethodSignature).parameterNames
        if (PROJECT_ID !in parameterNames ||
            USER_ID !in parameterNames
        ) {
            logger.warn(
                "Missing required parameters: " +
                    "$parameterValue|$parameterNames"
            )
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                defaultMessage = "projectId and userId are required."
            )
        }
        var projectId: String? = null
        var userId: String? = null
        parameterNames.forEachIndexed { index, name ->
            when (name) {
                PROJECT_ID ->
                    projectId = parameterValue[index].toString()
                USER_ID ->
                    userId = parameterValue[index].toString()
            }
        }
        if (userId.isNullOrEmpty() || projectId.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                defaultMessage =
                    "projectId or userId cannot be empty or null!"
            )
        }
        val isMember = permissionProjectService.isProjectMember(
            userId = userId,
            projectCode = projectId
        )
        if (!isMember) {
            throw PermissionForbiddenException(
                message = "用户[$userId]不是项目[$projectId]的成员"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            BkProjectMemberCheckAspect::class.java
        )
        private const val PROJECT_ID = "projectId"
        private const val USER_ID = "userId"
    }
}
