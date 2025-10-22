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

package com.tencent.devops.common.web.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.BkApiHandleService
import com.tencent.devops.common.web.utils.BkApiUtil
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class BkApiHandleProjectAccessServiceImpl : BkApiHandleService {

    companion object {
        private val logger = LoggerFactory.getLogger(BkApiHandleProjectAccessServiceImpl::class.java)
    }

    override fun handleBuildApiService(parameterNames: Array<String>, parameterValue: Array<Any>) {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return
        val request = attributes.request
        // 从请求头中取出项目信息
        var projectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        if (projectId.isNullOrBlank() && parameterNames.contains(KEY_PROJECT_ID)) {
            for (index in parameterValue.indices) {
                if (parameterNames[index] == KEY_PROJECT_ID) {
                    projectId = parameterValue[index].toString()
                    break
                }
            }
        }
        if (projectId.isNullOrBlank()) {
            return
        }
        val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
        // 判断项目是否在限制接口访问的列表中
        if (redisOperation.isMember(BkApiUtil.getApiAccessLimitProjectsKey(), projectId)) {
            val requestURI = request.requestURI
            logger.info("Project[$projectId] does not have access permission for interface[$requestURI]")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_PROJECT_API_ACCESS_NO_PERMISSION,
                params = arrayOf(projectId, requestURI)
            )
        }
    }
}
