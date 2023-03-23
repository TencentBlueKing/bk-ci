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

package com.tencent.devops.artifactory

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.ShortUrlService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoService
import com.tencent.devops.artifactory.service.bkrepo.GitCIBkRepoDownloadService
import com.tencent.devops.artifactory.service.permission.DefaultPipelineServiceImpl
import com.tencent.devops.artifactory.service.permission.RbacArtPipelineServiceImpl
import com.tencent.devops.artifactory.service.permission.StreamArtPipelineServiceImpl
import com.tencent.devops.artifactory.service.permission.TxV3ArtPipelineServiceImpl
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.config.CommonConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Suppress("ALL")
@Configuration
class ArtifactoryTencentServiceConfig {

    /**
     *  下载链接服务
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun gitciAgentUrlService(
        pipelineService: PipelineService,
        bkRepoService: BkRepoService,
        client: Client,
        bkRepoClient: BkRepoClient,
        commonConfig: CommonConfig,
        shortUrlService: ShortUrlService
    ) = GitCIBkRepoDownloadService(
        pipelineService = pipelineService,
        bkRepoService = bkRepoService,
        client = client,
        bkRepoClient = bkRepoClient,
        commonConfig = commonConfig,
        shortUrlService = shortUrlService
    )

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun bkRepoDownloadService(
        pipelineService: PipelineService,
        bkRepoService: BkRepoService,
        client: Client,
        bkRepoClient: BkRepoClient,
        commonConfig: CommonConfig,
        shortUrlService: ShortUrlService
    ) = BkRepoDownloadService(
        pipelineService = pipelineService,
        bkRepoService = bkRepoService,
        client = client,
        bkRepoClient = bkRepoClient,
        commonConfig = commonConfig,
        shortUrlService = shortUrlService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun defaultPipelineService(
        client: Client,
        pipelineAuthServiceCode: BSPipelineAuthServiceCode,
        bkAuthPermissionApi: BSAuthPermissionApi,
        authProjectApi: BSAuthProjectApi,
        artifactoryAuthServiceCode: BSRepoAuthServiceCode
    ) = DefaultPipelineServiceImpl(
        client, pipelineAuthServiceCode, bkAuthPermissionApi, authProjectApi, artifactoryAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitCIPipelineService(
        client: Client,
        tokenCheckService: ClientTokenService
    ) = StreamArtPipelineServiceImpl(client, tokenCheckService)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3ArtPipelineServiceImpl(
        client: Client,
        tokenCheckService: ClientTokenService
    ) = TxV3ArtPipelineServiceImpl(client, tokenCheckService)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacArtPipelineServiceImpl(
        client: Client,
        tokenCheckService: ClientTokenService
    ) = RbacArtPipelineServiceImpl(client, tokenCheckService)
}
