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

package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.constant.API_PERMISSION
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_PIPELINE_ID
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.enums.RequestChannelTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import jakarta.ws.rs.HttpMethod
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.ext.Provider
import org.springframework.core.annotation.AnnotationUtils

@Provider
@RequestFilter
class RequestProjectPermissionFilter(
    private val redisOperation: RedisOperation
) : ContainerRequestFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(RequestProjectPermissionFilter::class.java)
    }

    @Value("\${api.pipeline.permission.switch:false}")
    private val apiPipelinePermissionSwitch: Boolean = false

    @Value("\${api.project.permission.switch:false}")
    private val apiProjectPermissionSwitch: Boolean = false

    @Context
    private var resourceInfo: ResourceInfo? = null

    override fun filter(requestContext: ContainerRequestContext) {
        // 判断流水线或者项目的api权限校验开关是否打开
        if ((!apiPipelinePermissionSwitch && !apiProjectPermissionSwitch) || resourceInfo == null) {
            return
        }
        // 判断接口是否标注了免权限校验的注解
        val method = resourceInfo!!.resourceMethod
        val bkApiHandleTypes = AnnotationUtils.findAnnotation(method, BkApiPermission::class.java)?.types?.toList()
        val noAuthCheckFlag = bkApiHandleTypes?.contains(BkApiHandleType.API_NO_AUTH_CHECK) ?: false
        // 如果接口免权限校验、接口类型是get请求或者接口是build接口等情况无需做权限校验（未结束的构建需要调build接口才能完成）
        val url = requestContext.uriInfo.requestUri.path
        val channel = I18nUtil.getRequestChannel()
        // 获取该次接口调用是否需要权限校验标识
        val permissionFlag = requestContext.getHeaderString(API_PERMISSION)?.toBoolean() ?: false
        logger.info("url[$url],noAuthCheckFlag[$noAuthCheckFlag],channel[$channel],permissionFlag[$permissionFlag]")
        val noCheckFlag = noAuthCheckFlag || permissionFlag || requestContext.method.uppercase() == HttpMethod.GET ||
            channel == RequestChannelTypeEnum.BUILD.name
        if (noCheckFlag) {
            return
        }
        val uriInfo = requestContext.uriInfo
        // 校验流水线API接口访问权限
        validatePipelineApiAccessPermission(requestContext, uriInfo, url)
        // 校验项目API接口访问权限
        validateProjectApiAccessPermission(requestContext, uriInfo, url)
    }

    private fun validateProjectApiAccessPermission(
        requestContext: ContainerRequestContext,
        uriInfo: UriInfo,
        url: String
    ) {
        val projectId =
            (requestContext.getHeaderString(AUTH_HEADER_PROJECT_ID) ?: uriInfo.pathParameters.getFirst(KEY_PROJECT_ID)
            ?: uriInfo.queryParameters.getFirst(KEY_PROJECT_ID))?.toString()
        // 判断项目是否在限制接口访问的列表中
        if (apiProjectPermissionSwitch && !projectId.isNullOrBlank() && redisOperation.isMember(
                key = BkApiUtil.getApiAccessLimitProjectsKey(),
                item = projectId
            )
        ) {
            logger.info("Project[$projectId] does not have access permission for interface[$url]")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_PROJECT_API_ACCESS_NO_PERMISSION,
                params = arrayOf(projectId, url)
            )
        }
    }

    private fun validatePipelineApiAccessPermission(
        requestContext: ContainerRequestContext,
        uriInfo: UriInfo,
        url: String
    ) {
        val pipelineId =
            (requestContext.getHeaderString(AUTH_HEADER_PIPELINE_ID) ?: uriInfo.pathParameters.getFirst(KEY_PIPELINE_ID)
            ?: uriInfo.queryParameters.getFirst(KEY_PIPELINE_ID))?.toString()
        // 判断流水线是否在限制接口访问的列表中
        if (apiPipelinePermissionSwitch && !pipelineId.isNullOrBlank() && redisOperation.isMember(
                key = BkApiUtil.getApiAccessLimitPipelinesKey(),
                item = pipelineId
            )
        ) {
            logger.info("Pipeline[$pipelineId] does not have access permission for interface[$url]")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_PIPELINE_API_ACCESS_NO_PERMISSION,
                params = arrayOf(pipelineId, url)
            )
        }
    }
}
