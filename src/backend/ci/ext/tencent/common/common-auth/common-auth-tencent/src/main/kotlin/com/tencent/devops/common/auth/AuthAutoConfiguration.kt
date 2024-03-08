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

package com.tencent.devops.common.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.BSCCProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.BkCCProperties
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.auth.jmx.JmxAuthApi
import com.tencent.devops.common.auth.service.BkAccessTokenApi
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jmx.export.MBeanExporter

@Configuration
@ConditionalOnWebApplication
class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    fun jmxAuthApi(mBeanExporter: MBeanExporter) = JmxAuthApi(mBeanExporter)

    @Bean
    @Primary
    fun bkCCProperties() = BkCCProperties()

    @Bean
    @Primary
    fun bkCCProjectApi(
        pipelineAuthServiceCode: PipelineAuthServiceCode,
        bkCCProperties: BkCCProperties,
        objectMapper: ObjectMapper,
        authTokenApi: AuthTokenApi
    ) =
        BSCCProjectApi(bkCCProperties, objectMapper, authTokenApi, pipelineAuthServiceCode)

    /**
     * 获取蓝鲸accessToken依赖这个配置,所以不管使用哪个权限中心,都需要配置这个bean
     */
    @Bean
    fun bkAuthProperties() = BkAuthProperties()

    @Bean
    fun bkAccessTokenApi(
        bkAuthProperties: BkAuthProperties,
        objectMapper: ObjectMapper,
        redisOperation: RedisOperation
    ) = BkAccessTokenApi(
        bkAuthProperties = bkAuthProperties,
        objectMapper = objectMapper,
        redisOperation = redisOperation
    )
}
