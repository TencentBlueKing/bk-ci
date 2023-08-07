/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.enums.RequestChannelTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import javax.ws.rs.HttpMethod
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.ext.Provider

@Provider
@RequestFilter
class RequestProjectPermissionFilter : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(RequestProjectPermissionFilter::class.java)
    }

    @Value("\${api.project.permission.switch:false}")
    private val apiProjectPermissionSwitch: Boolean = false

    override fun filter(requestContext: ContainerRequestContext) {
        // 判断项目的api权限校验开关是否打开，如果是get请求或者是build接口无需做权限校验（未结束的构建需要调build接口才能完成）
        val url = requestContext.uriInfo.requestUri.path
        logger.info("url[$url],channel[${I18nUtil.getRequestChannel()}]")
        if (!apiProjectPermissionSwitch || requestContext.method.uppercase() == HttpMethod.GET ||
            I18nUtil.getRequestChannel() == RequestChannelTypeEnum.BUILD.name
        ) {
            return
        }
        val uriInfo = requestContext.uriInfo
        val projectId =
            (requestContext.getHeaderString(AUTH_HEADER_PROJECT_ID) ?: uriInfo.pathParameters.getFirst(KEY_PROJECT_ID)
            ?: uriInfo.queryParameters.getFirst(KEY_PROJECT_ID))?.toString() ?: return
        val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
        // 判断项目是否在限制接口访问的列表中
        if (redisOperation.isMember(BkApiUtil.getApiAccessLimitProjectKey(), projectId)) {
            val path = uriInfo.requestUri.path
            logger.info("Project[$projectId] does not have access permission for interface[$path]")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_PROJECT_API_ACCESS_NO_PERMISSION,
                params = arrayOf(projectId, path)
            )
        }
    }
}
