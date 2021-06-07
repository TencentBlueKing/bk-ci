/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

import com.tencent.devops.common.api.annotation.UserLogin
import com.tencent.devops.common.web.handler.*
import com.tencent.devops.common.web.security.PermissionAuthDynamicFeature
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
        logger.info("JerseyConfig-mapper-register-start")
        register(MissingKotlinParameterExceptionMapper::class.java)
        register(CodeCCExceptionMapper::class.java)
        register(ClientExceptionMapper::class.java)
        register(UnauthorizedExceptionMapper::class.java)
        register(JsonMappingExceptionMapper::class.java)
        register(RuntimeExceptionMapper::class.java)
        register(AllExceptionMapper::class.java)
        register(MultiPartFeature::class.java)
        register(ErrorCodeExceptionMapper::class.java)
        register(PermissionAuthDynamicFeature::class.java)
        register(ValidationExceptionMapper::class.java)
        register(IOExceptionMapper::class.java)
        logger.info("JerseyConfig-mapper-register-end")
        val restResources = applicationContext.getBeansWithAnnotation(RestResource::class.java)
        logger.info("JerseyConfig-RestResource-register-start")
        restResources.values.forEach {
            logger.info("RestResource: $it")
            register(it)
        }
        logger.info("JerseyConfig-RestResource-register-end")

        val containerRequestFilter = applicationContext.getBeansWithAnnotation(RequestFilter::class.java)
        logger.info("JerseyConfig-RequestFilter-register-start")
        containerRequestFilter.values.forEach {
            logger.info("RequestFilter: $it")
            register(it)
        }
        val userLoginRequestFilter = applicationContext.getBeansWithAnnotation(UserLogin::class.java)
        logger.info("userLoginConfig-RequestFilter-register-start")
        userLoginRequestFilter.values.forEach {
            logger.info("UserLoginFilter: $it")
            register(it)
        }
        logger.info("JerseyConfig-RequestFilter-register-end")
    }
}
