package com.tencent.devops.common.web.aop

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_NEED_PARAM_
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_NULL
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.service.ServiceUserProjectMemberPermissionResource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory

@Aspect
class BkProjectMemberCheckAspect constructor(
    private val client: Client,
) {

    @Pointcut("@annotation(com.tencent.devops.common.web.annotation.BkProjectMemberCheck)")
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
        checkParameterName(parameterNames)
        var projectId: String? = null
        var userId: String? = null
        parameterNames.forEachIndexed { index, name ->
            when (name) {
                PROJECT_ID -> projectId = parameterValue[index]?.toString() ?: ""
                USER_ID -> userId = parameterValue[index]?.toString() ?: ""
            }
        }

        checkParameterValue(userId, projectId)
        val isProjectMember = checkProjectMember(
            userId = userId!!,
            projectId = projectId!!
        )

        if (!isProjectMember) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_USER_NOT_EXIST_IN_PROJECT,
                params = arrayOf(projectId!!, userId!!)
            )
        }
    }

    private fun checkProjectMember(userId: String, projectId: String): Boolean {
        return client.get(ServiceUserProjectMemberPermissionResource::class)
            .checkMember(userId, projectId).data!!
    }

    private fun checkParameterValue(userId: String?, projectId: String?) {
        val (invalid, field) = when {
            projectId.isNullOrEmpty() -> true to PROJECT_ID
            userId.isNullOrEmpty() -> true to USER_ID
            else -> false to ""
        }
        if (invalid) {
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_NULL,
                params = arrayOf(field),
                defaultMessage = "projectId or userId cannot be empty or null!"
            )
        }
    }

    private fun checkParameterName(parameterNames: Array<String>) {
        val (invalid, field) = when {
            PROJECT_ID !in parameterNames -> true to PROJECT_ID
            USER_ID !in parameterNames -> true to USER_ID
            else -> false to ""
        }
        if (invalid) {
            logger.warn("The request parameters for this method are incorrect: $parameterNames")
            throw ErrorCodeException(
                errorCode = ERROR_NEED_PARAM_,
                params = arrayOf(field),
                defaultMessage = "The request parameters for this method are incorrect." +
                        "projectId and userId are required."
            )
        }
    }
}


