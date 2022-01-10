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

package com.tencent.devops.common.archive

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.archive.api.JFrogConfigProperties
import com.tencent.devops.common.archive.api.JFrogExecutionApi
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.api.JFrogStorageApi
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.service.config.CommonConfig
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-jfrog.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class JFrogAutoConfiguration {

    @Bean
    @Primary
    fun jFrogAllConfigProperties(): JFrogAllConfigProperties {
        return JFrogAllConfigProperties()
    }

    @Bean
    @Profile("prod", "nobuild_prod", "nobuild_prod_gray")
    fun jFrogConfigPropertiesProd(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
        jFrogAllConfigProperties.prodUrl,
        jFrogAllConfigProperties.prodUsername,
        jFrogAllConfigProperties.prodPassword
    )

    @Bean
    @Profile("test", "nobuild_test", "nobuild_test_gray")
    fun jFrogConfigPropertiesTest(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
        jFrogAllConfigProperties.testUrl,
        jFrogAllConfigProperties.testUsername,
        jFrogAllConfigProperties.testPassword
    )

    @Bean
    @Profile("exp")
    fun jFrogConfigPropertiesExp(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
        jFrogAllConfigProperties.devUrl,
        jFrogAllConfigProperties.devUsername,
        jFrogAllConfigProperties.devPassword
    )

    @Bean
    @Profile("dev", "dev-v3", "default", "nobuild_dev", "local", "codecc_dev")
    fun jFrogConfigPropertiesDev(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
        jFrogAllConfigProperties.devUrl,
        jFrogAllConfigProperties.devUsername,
        jFrogAllConfigProperties.devPassword
    )

    @Bean
    @Primary
    fun jFrogStorageApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogStorageApi {
        return JFrogStorageApi(jFrogConfigProperties, objectMapper)
    }

    @Bean
    @Primary
    fun jFrogExecutionApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogExecutionApi =
        JFrogExecutionApi(jFrogConfigProperties, objectMapper)

    @Bean
    @Primary
    fun jFrogPropertiesApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogPropertiesApi =
        JFrogPropertiesApi(jFrogConfigProperties, objectMapper)

    @Bean
    @Primary
    fun jFrogService(commonConfig: CommonConfig) = JfrogService(commonConfig)
}
