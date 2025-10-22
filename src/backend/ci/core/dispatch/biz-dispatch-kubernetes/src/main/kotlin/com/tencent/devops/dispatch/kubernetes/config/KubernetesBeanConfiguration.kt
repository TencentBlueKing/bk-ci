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

package com.tencent.devops.dispatch.kubernetes.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.kubernetes.bcs.client.BcsBuilderClient
import com.tencent.devops.dispatch.kubernetes.bcs.client.BcsClientCommon
import com.tencent.devops.dispatch.kubernetes.bcs.client.BcsJobClient
import com.tencent.devops.dispatch.kubernetes.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.kubernetes.bcs.service.BcsContainerService
import com.tencent.devops.dispatch.kubernetes.bcs.service.BcsJobService
import com.tencent.devops.dispatch.kubernetes.client.DeploymentClient
import com.tencent.devops.dispatch.kubernetes.client.IngressClient
import com.tencent.devops.dispatch.kubernetes.client.KubernetesBuilderClient
import com.tencent.devops.dispatch.kubernetes.client.KubernetesClientCommon
import com.tencent.devops.dispatch.kubernetes.client.KubernetesJobClient
import com.tencent.devops.dispatch.kubernetes.client.KubernetesTaskClient
import com.tencent.devops.dispatch.kubernetes.client.SecretClient
import com.tencent.devops.dispatch.kubernetes.client.ServiceClient
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.interfaces.CommonService
import com.tencent.devops.dispatch.kubernetes.service.CoreCommonService
import com.tencent.devops.dispatch.kubernetes.service.KubernetesContainerService
import com.tencent.devops.dispatch.kubernetes.service.KubernetesJobService
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class KubernetesBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean(CommonService::class)
    fun commonService() = CoreCommonService()

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesClientCommon(
        commonService: CommonService
    ) = KubernetesClientCommon(commonService)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesBuilderClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = KubernetesBuilderClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesJobClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = KubernetesJobClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesTaskClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = KubernetesTaskClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesContainerService(
        logsPrinter: LogsPrinter,
        commonConfig: CommonConfig,
        kubernetesTaskClient: KubernetesTaskClient,
        kubernetesBuilderClient: KubernetesBuilderClient,
        kubernetesJobClient: KubernetesJobClient
    ) = KubernetesContainerService(
        logsPrinter = logsPrinter,
        commonConfig = commonConfig,
        kubernetesTaskClient = kubernetesTaskClient,
        kubernetesBuilderClient = kubernetesBuilderClient,
        kubernetesJobClient = kubernetesJobClient
    )

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun kubernetesJobService(
        kubernetesJobClient: KubernetesJobClient
    ) = KubernetesJobService(kubernetesJobClient)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsClientCommon(
        commonService: CommonService
    ) = BcsClientCommon(commonService)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsBuilderClient(
        objectMapper: ObjectMapper,
        clientCommon: BcsClientCommon
    ) = BcsBuilderClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsJobClient(
        objectMapper: ObjectMapper,
        clientCommon: BcsClientCommon
    ) = BcsJobClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsTaskClient(
        objectMapper: ObjectMapper,
        clientCommon: BcsClientCommon
    ) = BcsTaskClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsContainerService(
        bcsBuilderClient: BcsBuilderClient,
        logsPrinter: LogsPrinter,
        bcsTaskClient: BcsTaskClient,
        commonConfig: CommonConfig
    ) = BcsContainerService(bcsBuilderClient, logsPrinter, bcsTaskClient, commonConfig)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bcs", name = ["enable"], havingValue = "true")
    fun bcsJobService(
        bcsJobClient: BcsJobClient
    ) = BcsJobService(bcsJobClient)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun deploymentClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = DeploymentClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun ingressClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = IngressClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun secretClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = SecretClient(objectMapper, clientCommon)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kubernetes", name = ["enable"], havingValue = "true")
    fun serviceClient(
        objectMapper: ObjectMapper,
        clientCommon: KubernetesClientCommon
    ) = ServiceClient(objectMapper, clientCommon)
}
