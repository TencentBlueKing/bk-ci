package com.tencent.devops.auth.aspect

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.UserProjectPermissionService
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.web.utils.I18nUtil
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory


@Aspect
class BkProjectMemberCheckAspect constructor(
    private val userProjectPermissionService: UserProjectPermissionService,
) {

    @Pointcut("@annotation(com.tencent.devops.common.auth.api.BkProjectMemberCheck)")
    fun pointCut() = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(BkProjectMemberCheckAspect::class.java)
        private const val PROJECT_ID = "projectId"
        private const val USER_ID = "userId"
    }


    @Before("pointCut()")
    fun doBefore(jp: JoinPoint) {
        val parameterValue = jp.args
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        if (PROJECT_ID !in parameterNames || USER_ID !in parameterNames) {
            logger.warn("The request parameters for this method are incorrect: $parameterValue|$parameterNames")
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                defaultMessage = "The request parameters for this method are incorrect." +
                        "projectId and userId are required."
            )
        }
        var projectId: String? = null
        var userId: String? = null
        parameterNames.forEachIndexed { index, name ->
            when (name) {
                PROJECT_ID -> projectId = parameterValue[index].toString()
                USER_ID -> userId = parameterValue[index].toString()
            }
        }
        if (userId.isNullOrEmpty() || projectId.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                defaultMessage = "projectId or userId cannot be empty or null!"
            )
        }
        val hasProjectManagePermission = userProjectPermissionService.checkMember(
            userId = userId!!,
            projectCode = projectId!!
        )

        if (!hasProjectManagePermission) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_USER_NOT_EXIST_IN_PROJECT)
            )
        }
    }
}


