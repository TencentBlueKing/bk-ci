/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.stream.pulsar.convert.PulsarMessageConverter
import com.tencent.devops.common.stream.pulsar.custom.PulsarConfigBeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.source.ConfigurationPropertyName
import org.springframework.cloud.function.json.JacksonMapper
import org.springframework.cloud.function.json.JsonMapper
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.converter.CompositeMessageConverter

@Configuration
@AutoConfigureAfter(
    name = ["org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration"]
)
class ExtendedBindingHandlerMappingsProviderConfiguration {
    @Bean
    fun pulsarExtendedPropertiesDefaultMappingsProvider(): MappingsProvider {
        return MappingsProvider {
            val mappings: MutableMap<ConfigurationPropertyName, ConfigurationPropertyName> = HashMap()
            mappings[ConfigurationPropertyName.of("spring.cloud.stream.pulsar.bindings")] =
                ConfigurationPropertyName.of("spring.cloud.stream.pulsar.default")
            mappings
        }
    }

    @Bean
    fun pulsarConfigBeanPostProcessor(): PulsarConfigBeanPostProcessor {
        return PulsarConfigBeanPostProcessor()
    }

    /**
     * if you want to customize a bean, please use this BeanName `PulsarMessageConverter.DEFAULT_NAME`.
     */
    @Bean(PulsarMessageConverter.DEFAULT_NAME)
    @ConditionalOnMissingBean(name = [PulsarMessageConverter.DEFAULT_NAME])
    fun pulsarMessageConverter(): CompositeMessageConverter {
        return PulsarMessageConverter().getMessageConverter()
    }

    /**
     * 覆盖ContextFunctionCatalogAutoConfiguration中的JsonMapper
     */
    @Bean
    @Primary
    fun jsonMapper(objectMapper: ObjectMapper): JsonMapper {
        return JacksonMapper(objectMapper)
    }
}
