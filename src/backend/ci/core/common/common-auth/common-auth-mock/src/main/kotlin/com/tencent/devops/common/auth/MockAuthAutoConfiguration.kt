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

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.MockAuthPermissionApi
import com.tencent.devops.common.auth.api.MockAuthProjectApi
import com.tencent.devops.common.auth.api.MockAuthResourceApi
import com.tencent.devops.common.auth.api.MockAuthTokenApi
import com.tencent.devops.common.auth.code.ArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.BcsAuthServiceCode
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.MockArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.MockBcsAuthServiceCode
import com.tencent.devops.common.auth.code.MockCodeAuthServiceCode
import com.tencent.devops.common.auth.code.MockEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.MockPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.MockPipelineGroupAuthServiceCode
import com.tencent.devops.common.auth.code.MockProjectAuthServiceCode
import com.tencent.devops.common.auth.code.MockQualityAuthServiceCode
import com.tencent.devops.common.auth.code.MockRepoAuthServiceCode
import com.tencent.devops.common.auth.code.MockTicketAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 兜底bean,可能存在有些auth.idProvider声明了这些bean,有些没有声明,没有的就需要创建一个mock bean
 * 注意: 这个类要放在其他声明auth.idProvider配置类之后
 *
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("TooManyFunctions")
class MockAuthAutoConfiguration {

    @Bean
//    @Primary
    @ConditionalOnMissingBean(AuthTokenApi::class)
    fun authTokenApi() = MockAuthTokenApi()

    @Bean
//    @Primary
    @ConditionalOnMissingBean(AuthPermissionApi::class)
    fun authPermissionApi() = MockAuthPermissionApi()

    @Bean
//    @Primary
    @ConditionalOnMissingBean(AuthResourceApi::class)
    fun authResourceApi(authTokenApi: MockAuthTokenApi) = MockAuthResourceApi()

    @Bean
//    @Primary
    @ConditionalOnMissingBean(AuthProjectApi::class)
    fun authProjectApi(bkAuthPermissionApi: MockAuthPermissionApi) = MockAuthProjectApi(bkAuthPermissionApi)

    @Bean
    @ConditionalOnMissingBean(BcsAuthServiceCode::class)
    fun bcsAuthServiceCode() = MockBcsAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(PipelineAuthServiceCode::class)
    fun pipelineAuthServiceCode() = MockPipelineAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(PipelineAuthServiceCode::class)
    fun pipelineGroupAuthServiceCode() = MockPipelineGroupAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(CodeAuthServiceCode::class)
    fun codeAuthServiceCode() = MockCodeAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(ProjectAuthServiceCode::class)
    fun projectAuthServiceCode() = MockProjectAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(EnvironmentAuthServiceCode::class)
    fun environmentAuthServiceCode() = MockEnvironmentAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(RepoAuthServiceCode::class)
    fun repoAuthServiceCode() = MockRepoAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(TicketAuthServiceCode::class)
    fun ticketAuthServiceCode() = MockTicketAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(QualityAuthServiceCode::class)
    fun qualityAuthServiceCode() = MockQualityAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(ArtifactoryAuthServiceCode::class)
    fun artifactoryAuthServiceCode() = MockArtifactoryAuthServiceCode()
}
