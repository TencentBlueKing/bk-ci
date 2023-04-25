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

package com.tencent.devops.statistics.config

import com.tencent.devops.common.db.config.DBBaseConfiguration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor
import javax.sql.DataSource

/**
 *
 * Powered By Tencent
 */
@Configuration
@Import(DataSourceConfig::class, DBBaseConfiguration::class)
class JooqConfiguration {

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun dslContext(
        configurationMap: Map<String?, DefaultConfiguration?>,
        injectionPoint: InjectionPoint
    ): DSLContext {
        val annotatedElement: AnnotatedElement = injectionPoint.annotatedElement
        if (Constructor::class.java.isAssignableFrom(annotatedElement::class.java)) {
            val declaringClass: Class<*> = (annotatedElement as Constructor<*>).declaringClass
            val packageName = declaringClass.getPackage().name
            val configuration: org.jooq.Configuration?
            configuration = when {
                packageName.contains(".process") -> {
                    configurationMap["processJooqConfiguration"]
                }
                packageName.contains(".project") -> {
                    configurationMap["projectJooqConfiguration"]
                }
                packageName.contains(".openapi") -> {
                    configurationMap["openapiJooqConfiguration"]
                }
                else -> {
                    throw NoSuchBeanDefinitionException("no jooq configuration")
                }
            }
            return DSL.using(configuration)
        }
        throw NoSuchBeanDefinitionException("no jooq configuration")
    }

    @Bean
    fun processJooqConfiguration(
        @Qualifier("shardingDataSource")
        shardingDataSource: DataSource,
        @Qualifier("bkJooqExecuteListenerProvider")
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        return generateDefaultConfiguration(shardingDataSource, bkJooqExecuteListenerProvider)
    }

    @Bean
    fun projectJooqConfiguration(
        @Qualifier("projectDataSource")
        projectDataSource: DataSource,
        @Qualifier("bkJooqExecuteListenerProvider")
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        return generateDefaultConfiguration(projectDataSource, bkJooqExecuteListenerProvider)
    }

    @Bean
    fun openapiJooqConfiguration(
        @Qualifier("openapiDataSource")
        openapiDataSource: DataSource,
        @Qualifier("bkJooqExecuteListenerProvider")
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        return generateDefaultConfiguration(openapiDataSource, bkJooqExecuteListenerProvider)
    }

    private fun generateDefaultConfiguration(
        dataSource: DataSource,
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        val configuration = DefaultConfiguration()
        configuration.set(SQLDialect.MYSQL)
        configuration.set(dataSource)
        configuration.settings().isRenderSchema = false
        configuration.set(bkJooqExecuteListenerProvider)
        return configuration
    }
}
