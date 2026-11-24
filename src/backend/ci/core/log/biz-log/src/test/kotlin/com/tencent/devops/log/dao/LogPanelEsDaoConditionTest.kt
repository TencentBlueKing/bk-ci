/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.dao

import com.tencent.devops.common.es.client.LogClient
import java.lang.reflect.Proxy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationClassPostProcessor
import org.springframework.context.annotation.DeferredImportSelector
import org.springframework.context.annotation.Import
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.type.AnnotationMetadata

/**
 * 验证 [LogPanelEsDao] 在 [LogClient] 由延后自动配置注册时，
 * 仍能按 `log.storage.type=elasticsearch` 正常注册 BeanDefinition。
 */
class LogPanelEsDaoConditionTest {

    @Test
    fun registersWhenElasticsearchEvenIfLogClientComesFromDeferredAutoConfig() {
        val registered = isLogPanelEsDaoRegistered(mapOf("log.storage.type" to "elasticsearch"))
        assertTrue(registered)
    }

    @Test
    fun skipsRegistrationWhenLucene() {
        val registered = isLogPanelEsDaoRegistered(mapOf("log.storage.type" to "lucene"))
        assertFalse(registered)
    }

    @Test
    fun skipsRegistrationWhenStorageTypeMissing() {
        val registered = isLogPanelEsDaoRegistered(emptyMap())
        assertFalse(registered)
    }

    private fun isLogPanelEsDaoRegistered(props: Map<String, Any>): Boolean {
        val env = StandardEnvironment().apply {
            propertySources.addFirst(MapPropertySource("test", props))
        }
        val beanFactory = DefaultListableBeanFactory()
        val reader = AnnotatedBeanDefinitionReader(beanFactory, env)
        reader.register(LogPanelEsDao::class.java, SimulatedAutoConfigEntry::class.java)

        val postProcessor = ConfigurationClassPostProcessor().apply {
            setEnvironment(env)
        }
        postProcessor.postProcessBeanDefinitionRegistry(beanFactory)
        return beanFactory.getBeanNamesForType(LogPanelEsDao::class.java, true, false).isNotEmpty()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(SimulatedDeferredLogClientSelector::class)
    class SimulatedAutoConfigEntry

    class SimulatedDeferredLogClientSelector : DeferredImportSelector {
        override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> {
            return arrayOf(SimulatedESAutoConfiguration::class.java.name)
        }
    }

    @Configuration(proxyBeanMethods = false)
    class SimulatedESAutoConfiguration {
        @Bean
        fun logClient(): LogClient = Proxy.newProxyInstance(
            LogClient::class.java.classLoader,
            arrayOf(LogClient::class.java)
        ) { _, _, _ -> null } as LogClient
    }
}
