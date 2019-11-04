/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.code.*
import com.tencent.devops.common.auth.jmx.JmxAuthApi
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.jmx.export.MBeanExporter

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AuthAutoConfiguration {

    @Bean
    @Primary
    fun bkAuthProperties() = BkAuthProperties()

    @Bean
    @Primary
    fun authProperties() = AuthProperties()

//    @Bean
//    fun bkAuthProperties(@Autowired authProperties: AuthProperties):BkAuthProperties {
//        return BkAuthProperties(
//            envName = authProperties.envName,
//            idProvider = authProperties.idProvider,
//            grantType = authProperties.grantType,
//            url = authProperties.url,
//            bcsSecret = authProperties.bcsSecret,
//            codeSecret = authProperties.codeSecret,
//            pipelineSecret = authProperties.pipelineSecret,
//            artifactorySecret = authProperties.artifactorySecret,
//            ticketSecret = authProperties.ticketSecret,
//            environmentSecret = authProperties.environmentSecret,
//            experienceSecret = authProperties.experienceSecret,
//            thirdPartyAgentSecret = authProperties.thirdPartyAgentSecret,
//            vsSecret = authProperties.vsSecret,
//            qualitySecret = authProperties.qualitySecret,
//            wetestSecret = authProperties.wetestSecret
//        )
//    }

    @Bean
    @Primary
    fun bsAuthTokenApi(bkAuthProperties: BkAuthProperties, objectMapper: ObjectMapper, redisOperation: RedisOperation) =
        BSAuthTokenApi(bkAuthProperties, objectMapper, redisOperation)

    @Bean
    @Primary
    fun bsAuthPermissionApi(
        bkAuthProperties: BkAuthProperties,
        objectMapper: ObjectMapper,
        bsAuthTokenApi: BSAuthTokenApi,
        jmxAuthApi: JmxAuthApi
    ) =
        BSAuthPermissionApi(bkAuthProperties, objectMapper, bsAuthTokenApi, jmxAuthApi)

    @Bean
    @Primary
    fun bsAuthResourceApi(
        bkAuthProperties: BkAuthProperties,
        objectMapper: ObjectMapper,
        bsAuthTokenApi: BSAuthTokenApi
    ) =
        BSAuthResourceApi(bkAuthProperties, objectMapper, bsAuthTokenApi)

    @Bean
    @Primary
    fun bsAuthProjectApi(
        bkAuthProperties: BkAuthProperties,
        objectMapper: ObjectMapper,
        bsAuthTokenApi: BSAuthTokenApi,
        bsCCProjectApi: BSCCProjectApi
    ) =
        BSAuthProjectApi(bkAuthProperties, objectMapper, bsAuthTokenApi, bsCCProjectApi)

    @Bean
    fun jmxAuthApi(mBeanExporter: MBeanExporter) = JmxAuthApi(mBeanExporter)

    @Bean
    fun bcsAuthServiceCode() = BSBcsAuthServiceCode()

    @Bean
    fun bsPipelineAuthServiceCode() = BSPipelineAuthServiceCode()

    @Bean
    fun codeAuthServiceCode() = BSCodeAuthServiceCode()

    @Bean
    fun vsAuthServiceCode() = BSVSAuthServiceCode()

    @Bean
    fun environmentAuthServiceCode() = BSEnvironmentAuthServiceCode()

    @Bean
    fun repoAuthServiceCode() = BSRepoAuthServiceCode()

    @Bean
    fun ticketAuthServiceCode() = BSTicketAuthServiceCode()

    @Bean
    fun qualityAuthServiceCode() = BSQualityAuthServiceCode()

    @Bean
    fun wetestAuthServiceCode() = BSWetestAuthServiceCode()

    @Bean
    fun experienceAuthServiceCode() = BSExperienceAuthServiceCode()

    @Bean
    fun projectAuthSeriviceCode() = BSProjectServiceCodec()

    @Bean
    fun artifactoryAuthServiceCode() = BSArtifactoryAuthServiceCode()
}