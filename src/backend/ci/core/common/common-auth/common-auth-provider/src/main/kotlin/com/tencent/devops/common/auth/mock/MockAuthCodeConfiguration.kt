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

package com.tencent.devops.common.auth.mock

import com.tencent.devops.common.auth.code.ArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.BcsAuthServiceCode
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.ExperienceAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineGroupAuthServiceCode
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockBcsAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockCodeAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockExperienceAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockPipelineAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockPipelineGroupAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockProjectAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockQualityAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockRepoAuthServiceCode
import com.tencent.devops.common.auth.mock.code.MockTicketAuthServiceCode
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * mock serviceCode
 */
@Configuration
class MockAuthCodeConfiguration {
    @Bean
    @ConditionalOnMissingBean(BcsAuthServiceCode::class)
    fun bcsAuthServiceCode() = MockBcsAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(PipelineAuthServiceCode::class)
    fun pipelineAuthServiceCode() = MockPipelineAuthServiceCode()

    @Bean
    @ConditionalOnMissingBean(PipelineGroupAuthServiceCode::class)
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

    @Bean
    @ConditionalOnMissingBean(ExperienceAuthServiceCode::class)
    fun experienceAuthServiceCode() = MockExperienceAuthServiceCode()
}
