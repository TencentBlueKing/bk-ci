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

package com.tencent.devops.misc.config

import com.tencent.devops.common.db.config.DBBaseConfiguration
import org.jooq.DSLContext
import org.jooq.ExecuteListenerProvider
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
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
@AutoConfigureAfter(DBBaseConfiguration::class)
class JooqConfiguration {

    @Value("\${spring.datasource.misc.pkgRegex:}")
    private val pkgRegex = "\\.(process|project|repository|dispatch|plugin|quality|artifactory|environment|gpt)"

    companion object {
        private val LOG = LoggerFactory.getLogger(JooqConfiguration::class.java)
    }

    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun dslContext(
        configurationMap: Map<String?, DefaultConfiguration?>,
        injectionPoint: InjectionPoint
    ): DSLContext? {
        val annotatedElement: AnnotatedElement = injectionPoint.annotatedElement
        if (Constructor::class.java.isAssignableFrom(annotatedElement::class.java)) {
            val declaringClass: Class<*> = (annotatedElement as Constructor<*>).declaringClass
            val packageName = declaringClass.getPackage().name
            val matchResult = pkgRegex.toRegex().findAll(packageName)
            if (matchResult.any()) {
                val module = matchResult.last().value.substring(1)
                val configuration = configurationMap["${module}JooqConfiguration"]
                return if (configuration != null) {
                    LOG.info("dslContext_init|${module}JooqConfiguration|${declaringClass.name}")
                    DSL.using(configuration)
                } else {
                    null
                }
            }
        }
        return DSL.using(configurationMap["defaultJooqConfiguration"]!!)
    }

    @Bean
    fun processJooqConfiguration(
        @Qualifier("shardingDataSource")
        shardingDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(shardingDataSource, executeListenerProviders)
    }

    @Bean
    fun projectJooqConfiguration(
        @Qualifier("projectDataSource")
        projectDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(projectDataSource, executeListenerProviders)
    }

    @Bean
    fun repositoryJooqConfiguration(
        @Qualifier("repositoryDataSource")
        repositoryDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(repositoryDataSource, executeListenerProviders)
    }

    @Bean
    fun dispatchJooqConfiguration(
        @Qualifier("dispatchDataSource")
        dispatchDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(dispatchDataSource, executeListenerProviders)
    }

    @Bean
    fun pluginJooqConfiguration(
        @Qualifier("pluginDataSource")
        pluginDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(pluginDataSource, executeListenerProviders)
    }

    @Bean
    fun gptJooqConfiguration(
        @Qualifier("pluginDataSource")
        pluginDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(pluginDataSource, executeListenerProviders)
    }

    @Bean
    fun qualityJooqConfiguration(
        @Qualifier("qualityDataSource")
        qualityDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(qualityDataSource, executeListenerProviders)
    }

    @Bean
    fun artifactoryJooqConfiguration(
        @Qualifier("artifactoryDataSource")
        artifactoryDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(artifactoryDataSource, executeListenerProviders)
    }

    @Bean
    fun environmentJooqConfiguration(
        @Qualifier("environmentDataSource")
        environmentDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(environmentDataSource, executeListenerProviders)
    }

    @Bean
    fun imageJooqConfiguration(
        @Qualifier("imageDataSource")
        imageDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        return generateDefaultConfiguration(imageDataSource, executeListenerProviders)
    }

    private fun generateDefaultConfiguration(
        dataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DefaultConfiguration {
        val configuration = DefaultConfiguration()
        configuration.set(SQLDialect.MYSQL)
        configuration.set(dataSource)
        configuration.settings().isRenderSchema = false
        configuration.set(*executeListenerProviders.stream().toArray { size ->
            arrayOfNulls<ExecuteListenerProvider>(size)
        })
        return configuration
    }
}
