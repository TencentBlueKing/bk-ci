package com.tencent.devops.common.web.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_NEED_PARAM_
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_NULL
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.BkApiHandleService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class BkApiHandleProjectMemberCheckServiceImpl : BkApiHandleService {


    companion object {
        private val logger = LoggerFactory.getLogger(BkApiHandleProjectMemberCheckServiceImpl::class.java)
        private const val PROJECT_ID = "projectId"
        private const val USER_ID = "userId"
    }

    override fun handleBuildApiService(parameterNames: Array<String>, parameterValue: Array<Any>) {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw IllegalStateException("No active ServletRequestAttributes")
        val request = attributes.request
        // 从请求头中取出项目id和用户id
        val projectId = fetchRequiredParam(
            paramName = PROJECT_ID,
            headerName = AUTH_HEADER_DEVOPS_PROJECT_ID,
            parameterNames = parameterNames,
            parameterValue = parameterValue,
            request = request
        )

        val userId = fetchRequiredParam(
            paramName = USER_ID,
            headerName = AUTH_HEADER_USER_ID,
            parameterNames = parameterNames,
            parameterValue = parameterValue,
            request = request
        )




    }

    private fun fetchRequiredParam(
        paramName: String,
        headerName: String,
        parameterNames: Array<String>,
        parameterValue: Array<Any>,
        request: HttpServletRequest
    ): String {
        // 优先从Header中获取
        request.getHeader(headerName)?.takeIf { it.isNotBlank() }?.let { return it }

        val index = parameterNames.indexOf(paramName).takeIf { it != -1 }
            ?: throw ErrorCodeException(
                errorCode = ERROR_NEED_PARAM_,
                params = arrayOf(paramName),
                defaultMessage = "The request parameters for this method are incorrect." +
                        "projectId and userId are required."
            )

        return parameterValue[index].toString().takeIf { it.isNotBlank() }
            ?: throw ErrorCodeException(
                errorCode = PARAMETER_IS_NULL,
                params = arrayOf(paramName),
                defaultMessage = "'$paramName' cannot be null or blank!"
            )
    }

    private fun checkProjectMember(userId: String, projectId: String) {
        SpringContextUtil.getBean(AuthProjectApi::class.java).checkProjectUser()

    }
}