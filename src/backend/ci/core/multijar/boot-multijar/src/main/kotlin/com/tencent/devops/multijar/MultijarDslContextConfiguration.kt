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
package com.tencent.devops.multijar

import com.tencent.devops.common.db.config.DBBaseConfiguration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 *
 * Powered By Tencent
 */
@Configuration
@Import(DBBaseConfiguration::class, DataSourceDefinitionRegistrar::class, JooqDefinitionRegistrar::class)
class MultijarDslContextConfiguration {

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun dslContext(
        configurationMap: Map<String?, DefaultConfiguration?>,
        injectionPoint: InjectionPoint
    ): DSLContext {
        val annotatedElement = injectionPoint.annotatedElement
        validateAnnotatedElementType(annotatedElement)

        val declaringClass = when (annotatedElement) {
            is Constructor<*> -> annotatedElement.declaringClass
            is Method -> annotatedElement.declaringClass
            is Field -> annotatedElement.declaringClass
            else -> throw IllegalArgumentException("Invalid annotatedElement type")
        }

        val currentPackageName = declaringClass.`package`.name
        val matchingModuleName = multiModuleName.find { currentPackageName.contains(it) }
            ?: throw NoSuchBeanDefinitionException("no jooq configuration")

        val configurationName = when {
            currentPackageName.contains(".misc") -> {
                val matchResult = miscServiceRegex.find(currentPackageName)
                "${matchResult?.groupValues?.get(1) ?: "default"}JooqConfiguration"
            }
            currentPackageName.contains(".store") -> "storeJooqConfiguration"
            currentPackageName.contains(".dispatch") -> handleDispatchModuleDslContext(currentPackageName)
            else -> matchingModuleName.plus("JooqConfiguration")
        }

        val configuration = configurationMap[configurationName]
            ?: throw NoSuchBeanDefinitionException("no $configurationName $currentPackageName")

        return DSL.using(configuration)
    }

    fun validateAnnotatedElementType(annotatedElement: AnnotatedElement) {
        if (annotatedElement !is Constructor<*> && annotatedElement !is Method && annotatedElement !is Field) {
            throw IllegalArgumentException("Invalid annotatedElement type")
        }
    }

    fun handleDispatchModuleDslContext(packageName: String): String {
        return when {
            packageName.startsWith("com.tencent.devops.dispatch.kubernetes") ||
                packageName.startsWith("com.tencent.devops.dispatch.startcloud") ||
                packageName.startsWith("com.tencent.devops.dispatch.devcloud") -> {
                logger.info("dslContext Configuration: dispatchKubernetesJooqConfiguration")
                "dispatchKubernetesJooqConfiguration"
            }
            packageName.startsWith("com.tencent.devops.dispatch.") -> {
                logger.info("dslContext Configuration: dispatchJooqConfiguration|$packageName")
                "dispatchJooqConfiguration"
            }
            else -> "defaultJooqConfiguration"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MultijarDslContextConfiguration::class.java)
        private val multiModuleName = System.getProperty("devops.multi.from").split(",")
        private val miscServiceRegex = ("\\.(process|project|repository|dispatch|plugin" +
            "|quality|artifactory|environment)").toRegex()
    }
}
