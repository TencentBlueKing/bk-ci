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

package com.tencent.devops.common.redis

import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.redis.split.RedisSplitProperties
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.lettuce.core.resource.ClientResources
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(RedisAutoConfiguration::class)
@AutoConfigureAfter(name = ["org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration"])
@EnableConfigurationProperties(RedisSplitProperties::class)
class RedisAutoConfiguration {

    @Value("\${spring.redis.name:#{null}}")
    private var redisName: String? = null

    @Value("\${spring.data.redis.split.enabled:false}")
    private var splitEnabled: Boolean = false

    @Primary
    @Bean("redisOperation")
    fun redisOperation(
        redisConnectionFactory: RedisConnectionFactory,
        redisSplitProperties: RedisSplitProperties
    ): RedisOperation {
        val redisTemplate = getRedisTemplate(redisConnectionFactory)
        val slaveRedisConnectionFactory = slaveRedisConnectionFactory(redisConnectionFactory, redisSplitProperties)
        val slaveRedisTemplate = slaveRedisConnectionFactory?.let { getRedisTemplate(it) }
        return RedisOperation(
            masterRedisTemplate = redisTemplate,
            slaveRedisTemplate = slaveRedisTemplate,
            splitMode = redisSplitProperties.mode,
            redisName = redisName
        )
    }

    @Bean("redisStringHashOperation")
    fun redisStringHashOperation(
        redisConnectionFactory: RedisConnectionFactory,
        redisSplitProperties: RedisSplitProperties
    ): RedisOperation {
        val redisTemplate = getRedisTemplate(redisConnectionFactory, true)
        val slaveRedisConnectionFactory = slaveRedisConnectionFactory(redisConnectionFactory, redisSplitProperties)
        val slaveRedisTemplate = slaveRedisConnectionFactory?.let { getRedisTemplate(it, true) }
        return RedisOperation(
            masterRedisTemplate = redisTemplate,
            slaveRedisTemplate = slaveRedisTemplate,
            splitMode = redisSplitProperties.mode,
            redisName = redisName
        )
    }

    fun slaveRedisConnectionFactory(
        redisConnectionFactory: RedisConnectionFactory,
        redisSplitProperties: RedisSplitProperties
    ): LettuceConnectionFactory? {
        if (!splitEnabled) {
            logger.info("redis split is disabled")
            return null
        }
        return if (redisConnectionFactory is LettuceConnectionFactory) {
            val masterClientConfiguration = redisConnectionFactory.clientConfiguration // 复用主库的lettuce配置

            val slaveConfiguration = RedisStandaloneConfiguration()
            slaveConfiguration.hostName = redisSplitProperties.host!!
            slaveConfiguration.port = redisSplitProperties.port!!
            slaveConfiguration.password = RedisPassword.of(redisSplitProperties.password!!)
            slaveConfiguration.database = redisSplitProperties.database!!

            val lettuceConnectionFactory = LettuceConnectionFactory(slaveConfiguration, masterClientConfiguration)
            lettuceConnectionFactory.afterPropertiesSet()
            lettuceConnectionFactory
        } else {
            throw RuntimeException("Redis split just support lettuce")
        }
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

    private fun getRedisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        setHashSerializer: Boolean = false
    ): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.setConnectionFactory(redisConnectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        if (setHashSerializer) {
            template.hashValueSerializer = StringRedisSerializer()
            template.hashKeySerializer = StringRedisSerializer()
        }
        template.afterPropertiesSet()
        return template
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisAutoConfiguration::class.java)
    }
}
