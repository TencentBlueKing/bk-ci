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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web

import com.tencent.devops.common.web.handler.AllExceptionMapper
import com.tencent.devops.common.web.handler.BadRequestExceptionMapper
import com.tencent.devops.common.web.handler.ClientExceptionMapper
import com.tencent.devops.common.web.handler.CodeccReportExceptionMapper
import com.tencent.devops.common.web.handler.CustomExceptionMapper
import com.tencent.devops.common.web.handler.DependNotFoundExceptionMapper
import com.tencent.devops.common.web.handler.ErrorCodeExceptionMapper
import com.tencent.devops.common.web.handler.IllegalArgumentExceptionMapper
import com.tencent.devops.common.web.handler.JsonMappingExceptionMapper
import com.tencent.devops.common.web.handler.MissingKotlinParameterExceptionMapper
import com.tencent.devops.common.web.handler.NotFoundExceptionMapper
import com.tencent.devops.common.web.handler.OperationExceptionMapper
import com.tencent.devops.common.web.handler.ParamBlankExceptionMapper
import com.tencent.devops.common.web.handler.ParamExceptionMapper
import com.tencent.devops.common.web.handler.PermissionForbiddenExceptionMapper
import com.tencent.devops.common.web.handler.PipelineAlreadyExistExceptionMapper
import com.tencent.devops.common.web.handler.RemoteServiceExceptionMapper
import com.tencent.devops.common.web.handler.RuntimeExceptionMapper
import com.tencent.devops.common.web.handler.UnauthorizedExceptionMapper
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import javax.ws.rs.ApplicationPath

@ApplicationPath("/api")
open class JerseyConfig : ResourceConfig(), ApplicationContextAware, InitializingBean {
    private lateinit var applicationContext: ApplicationContext
    private val logger = LoggerFactory.getLogger(JerseyConfig::class.java)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterPropertiesSet() {
        logger.info("JerseyConfig-register-start")
        register(DependNotFoundExceptionMapper::class.java)
        register(ParamBlankExceptionMapper::class.java)
        register(IllegalArgumentExceptionMapper::class.java)
        register(ParamExceptionMapper::class.java)
        register(MissingKotlinParameterExceptionMapper::class.java)
        register(BadRequestExceptionMapper::class.java)
        register(NotFoundExceptionMapper::class.java)
        register(ClientExceptionMapper::class.java)
        register(RemoteServiceExceptionMapper::class.java)
        register(OperationExceptionMapper::class.java)
        register(UnauthorizedExceptionMapper::class.java)
        register(JsonMappingExceptionMapper::class.java)
        register(RuntimeExceptionMapper::class.java)
        register(AllExceptionMapper::class.java)
        register(MultiPartFeature::class.java)
        register(PipelineAlreadyExistExceptionMapper::class.java)
        register(ErrorCodeExceptionMapper::class.java)
        register(CustomExceptionMapper::class.java)
        register(PermissionForbiddenExceptionMapper::class.java)
        register(CodeccReportExceptionMapper::class.java)
        register(ErrorCodeExceptionMapper::class.java)
        logger.info("JerseyConfig-RestResource-find-start")
        val restResources = applicationContext.getBeansWithAnnotation(RestResource::class.java)
        logger.info("JerseyConfig-RestResource-register-start")
        restResources.values.forEach {
            logger.info("RestResource: $it")
            register(it)
        }
        logger.info("JerseyConfig-RestResource-register-end")
        val containerRequestFilter = applicationContext.getBeansWithAnnotation(RequestFilter::class.java)
        containerRequestFilter.values.forEach {
            logger.info("RequestFilter: $it")
            register(it)
        }
        logger.info("JerseyConfig-RequestFilter-register-end")
    }
}
