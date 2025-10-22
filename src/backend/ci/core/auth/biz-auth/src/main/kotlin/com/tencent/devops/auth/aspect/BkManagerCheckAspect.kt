/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.auth.aspect

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.iam.PermissionProjectService
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
import org.springframework.stereotype.Component

@Aspect
@Component
class BkManagerCheckAspect constructor(
    private val permissionProjectService: PermissionProjectService
) {
    @Pointcut("@annotation(com.tencent.devops.common.auth.api.BkManagerCheck)")
    fun pointCut() = Unit

    /**
     * Before advice: Executed before the target method
     *
     * @param jp ProceedingJoinPoint
     */
    @Before("pointCut()")
    fun checkManager(jp: JoinPoint) {
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
        val hasProjectManagePermission = permissionProjectService.checkProjectManager(
            userId = userId!!,
            projectCode = projectId!!
        )
        if (!hasProjectManagePermission) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkManagerCheckAspect::class.java)
        private const val PROJECT_ID = "projectId"
        private const val USER_ID = "userId"
    }
}
