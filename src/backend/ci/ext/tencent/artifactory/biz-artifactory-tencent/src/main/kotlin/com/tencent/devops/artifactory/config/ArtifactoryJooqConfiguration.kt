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

package com.tencent.devops.artifactory.config

import com.tencent.devops.common.db.config.DBBaseConfiguration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import javax.sql.DataSource

/**
 *
 * Powered By Tencent
 */
@Configuration
@Import(ArtifactoryDataSourceConfig::class, DBBaseConfiguration::class)
@ConditionalOnMissingClass("com.tencent.devops.multijar.MultijarDslContextConfiguration")
@SuppressWarnings("ReturnCount")
class ArtifactoryJooqConfiguration {

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactoryJooqConfiguration::class.java)
    }

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun dslContext(
        configurationMap: Map<String?, DefaultConfiguration?>,
        injectionPoint: InjectionPoint
    ): DSLContext? {
        val declaringClass = when (val annotatedElement: AnnotatedElement = injectionPoint.annotatedElement) {
            is Constructor<*> -> annotatedElement.declaringClass
            is Method -> annotatedElement.declaringClass
            is Field -> annotatedElement.declaringClass
            else -> return DSL.using(configurationMap["defaultJooqConfiguration"]!!)
        }
        val packageName = declaringClass.getPackage().name

        if (packageName.startsWith("com.tencent.devops.artifactory")) {
            val configuration = configurationMap["artifactoryJooqConfig"]
                ?: throw NoSuchBeanDefinitionException("no artifactoryJooqConfig")
            LOG.info("dslContext_init|artifactoryJooqConfig|${declaringClass.name}")
            return DSL.using(configuration)
        } else if (packageName.startsWith("com.tencent.devops.experience")) {
            val configuration = configurationMap["experienceJooqConfig"]
                ?: throw NoSuchBeanDefinitionException("no experienceJooqConfig")
            LOG.info("dslContext_init|experienceJooqConfig|${declaringClass.name}")
            return DSL.using(configuration)
        } else {
            LOG.error("dslContext_init error , packageName: $packageName is not config!")
            return DSL.using(configurationMap["defaultJooqConfiguration"]!!)
        }
    }

    @Bean
    fun experienceJooqConfig(
        @Qualifier("experienceDataSource")
        shardingDataSource: DataSource,
        @Qualifier("bkJooqExecuteListenerProvider")
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        return generateDefaultConfiguration(shardingDataSource, bkJooqExecuteListenerProvider)
    }

    @Bean
    fun artifactoryJooqConfig(
        @Qualifier("artifactoryDataSource")
        artifactoryDataSource: DataSource,
        @Qualifier("bkJooqExecuteListenerProvider")
        bkJooqExecuteListenerProvider: DefaultExecuteListenerProvider
    ): DefaultConfiguration {
        return generateDefaultConfiguration(artifactoryDataSource, bkJooqExecuteListenerProvider)
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
