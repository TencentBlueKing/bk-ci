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

package com.tencent.devops.common.web

import com.tencent.devops.common.web.annotation.BkExceptionMapper
import com.tencent.devops.common.web.interceptor.BkWriterInterceptor
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.ResourceConfig
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import java.lang.reflect.Modifier
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
        register(ValidationConfigurationContextResolver::class.java)
        register(MultiPartFeature::class.java)
        register(BkWriterInterceptor::class.java)
        logger.info("JerseyConfig-ExceptionMapper-Spring-find-start")
        val mappers = applicationContext.getBeansWithAnnotation(BkExceptionMapper::class.java)
        logger.info("JerseyConfig-ExceptionMapper-register-start")
        mappers.values.forEach {
            logger.info("ExceptionMapper: $it")
            register(it)
        }
        logger.info("JerseyConfig-BkExceptionMapper-Reflect-find-start")
        val reflections = Reflections("com.tencent.devops.common.web.handler")
        val handlerClasses = reflections.getTypesAnnotatedWith(BkExceptionMapper::class.java)
        handlerClasses?.forEach { handlerClazz ->
            if (!Modifier.isAbstract(handlerClazz.modifiers)) {
                logger.info("Reflect-BkExceptionMapper: $handlerClazz")
                register(handlerClazz)
            }
        }
        logger.info("JerseyConfig-ExceptionMapper-register-end")
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
