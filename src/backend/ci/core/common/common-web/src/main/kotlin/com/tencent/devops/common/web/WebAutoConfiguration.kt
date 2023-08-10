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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.interceptor.BkWriterInterceptor
import com.tencent.devops.common.web.jasypt.DefaultEncryptor
import io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider
import io.undertow.UndertowOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered
import org.springframework.core.env.Environment

/**
 *
 * Powered By Tencent
 */
@Suppress("ALL")
@Configuration
@PropertySource("classpath:/common-web.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(JerseyAutoConfiguration::class)
@EnableConfigurationProperties(SwaggerProperties::class)
@DependsOn("globalProxyConfiguration")
class WebAutoConfiguration {

    @Bean
    @Profile("prod")
    fun jerseyConfig() = JerseyConfig()

    @Bean
    @Profile("!prod")
    fun jerseySwaggerConfig() = JerseySwaggerConfig()

    @Bean
    @Primary
    fun jerseyTagsProvider(): JerseyTagsProvider {
        return BkJerseyTagProvider()
    }

    @Bean
    @Primary
    fun objectMapper() = JsonUtil.getObjectMapper()

    @Bean("jasyptStringEncryptor")
    @Primary
    fun stringEncryptor(@Value("\${enc.key:rAFOey00bcuMNMrt}") key: String) = DefaultEncryptor(key)

    @Bean
    fun versionInfoResource() = VersionInfoResource()

    @Bean
    fun jmxAutoConfiguration(environment: Environment) = JmxAutoConfiguration(environment)

    @Bean
    fun bkWriterInterceptor() = BkWriterInterceptor()

    @Bean
    @ConditionalOnProperty(
        prefix = "server.undertow.accesslog",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun undertowServletWebServerFactory(
        @Value("\${server.undertow.accesslog.pattern:}") pattern: String
    ): UndertowServletWebServerFactory? {
        logger.info("undertowServletWebServerFactory|init|pattern=$pattern")
        val factory = UndertowServletWebServerFactory()
        if (pattern.contains("%D") || pattern.contains("%T")) {
            factory.addBuilderCustomizers(UndertowBuilderCustomizer { builder ->
                builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true)
            })
        }
        return factory
    }

    private val logger = LoggerFactory.getLogger(WebAutoConfiguration::class.java)
}
