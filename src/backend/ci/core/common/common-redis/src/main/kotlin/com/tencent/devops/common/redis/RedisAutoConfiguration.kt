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

package com.tencent.devops.common.redis

import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(RedisAutoConfiguration::class)
class RedisAutoConfiguration {

    @Value("\${spring.redis.name:#{null}}")
    private var redisName: String? = null

    @Primary
    @Bean("redisOperation")
    fun redisOperation(@Autowired factory: RedisConnectionFactory): RedisOperation {
        val template = RedisTemplate<String, String>()
        template.setConnectionFactory(factory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return RedisOperation(template, redisName)
    }

    @Bean("redisStringHashOperation")
    fun redisStringHashOperation(@Autowired factory: RedisConnectionFactory): RedisOperation {
        val template = RedisTemplate<String, String>()
        template.setConnectionFactory(factory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return RedisOperation(template, redisName)
    }

    @Bean
    fun simpleRateLimiter(@Autowired redisOperation: RedisOperation): SimpleRateLimiter {
        return SimpleRateLimiter(redisOperation)
    }

    @Bean
    @Primary
    fun lettuceMetrics(meterRegistry: MeterRegistry): ClientResourcesBuilderCustomizer {
        val options = MicrometerOptions.builder().build()
        return ClientResourcesBuilderCustomizer { client: ClientResources.Builder ->
            client.commandLatencyRecorder(
                MicrometerCommandLatencyRecorder(meterRegistry, options)
            )
        }
    }
}
