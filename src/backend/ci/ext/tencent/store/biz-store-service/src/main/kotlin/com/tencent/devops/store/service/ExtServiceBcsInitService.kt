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

package com.tencent.devops.store.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.api.ServiceBcsResource
import com.tencent.devops.dispatch.pojo.CreateBcsNameSpaceRequest
import com.tencent.devops.dispatch.pojo.CreateImagePullSecretRequest
import com.tencent.devops.dispatch.pojo.KubernetesLabel
import com.tencent.devops.dispatch.pojo.KubernetesLimitRange
import com.tencent.devops.dispatch.pojo.KubernetesRepo
import com.tencent.devops.store.config.ExtServiceBcsConfig
import com.tencent.devops.store.config.ExtServiceBcsLimitRangeConfig
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@DependsOn("springContextUtil")
class ExtServiceBcsInitService @Autowired constructor(
    private val client: Client,
    private val extServiceBcsConfig: ExtServiceBcsConfig,
    private val extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig,
    private val extServiceBcsLimitRangeConfig: ExtServiceBcsLimitRangeConfig,
    private val extServiceImageSecretConfig: ExtServiceImageSecretConfig
) {

    private val logger = LoggerFactory.getLogger(ExtServiceBcsInitService::class.java)

    @PostConstruct
    fun initBcsNamespace() {
        logger.info("begin execute initBcsNamespace")
        // 初始化bcs命名空间（包括已发布扩展服务版本的命名空间和处于测试中扩展服务版本的命名空间）
        val namespaceName = extServiceBcsNameSpaceConfig.namespaceName
        val createBcsNameSpaceRequest = CreateBcsNameSpaceRequest(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            kubernetesLabel = KubernetesLabel(
                labelKey = extServiceBcsNameSpaceConfig.labelKey,
                labelValue = extServiceBcsNameSpaceConfig.labelValue
            ),
            limitRangeInfo = KubernetesLimitRange(
                defaultCpu = extServiceBcsLimitRangeConfig.defaultCpu,
                defaultMemory = extServiceBcsLimitRangeConfig.defaultMemory,
                defaultRequestCpu = extServiceBcsLimitRangeConfig.defaultRequestCpu,
                defaultRequestMemory = extServiceBcsLimitRangeConfig.defaultRequestMemory,
                limitType = extServiceBcsLimitRangeConfig.limitType
            )
        )
        // 创建已发布扩展服务版本的命名空间
        val releaseNamespaceResult =
            client.get(ServiceBcsResource::class).createNamespace(namespaceName, createBcsNameSpaceRequest)
        logger.info("create namespace:$namespaceName result is:$releaseNamespaceResult")
        val grayNamespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName
        // 创建测试中扩展服务版本的命名空间
        val grayNamespaceResult =
            client.get(ServiceBcsResource::class).createNamespace(grayNamespaceName, createBcsNameSpaceRequest)
        logger.info("create namespace:$grayNamespaceName result is:$grayNamespaceResult")
        logger.info("end execute initBcsNamespace")
    }

    @PostConstruct
    fun initBcsImagePullSecret() {
        logger.info("begin execute initBcsImagePullSecret")
        val secretName = extServiceImageSecretConfig.secretName
        val createImagePullSecretRequest = CreateImagePullSecretRequest(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            kubernetesRepo = KubernetesRepo(
                registryUrl = extServiceImageSecretConfig.repoRegistryUrl,
                username = extServiceImageSecretConfig.repoUsername,
                password = extServiceImageSecretConfig.repoPassword,
                email = extServiceImageSecretConfig.repoEmail
            )
        )
        // 创建已发布扩展服务版本的命名空间拉取镜像secret
        val createReleaseNsImagePullSecretResult = client.get(ServiceBcsResource::class).createImagePullSecretTest(
            namespaceName = extServiceBcsNameSpaceConfig.namespaceName,
            secretName = secretName,
            createImagePullSecretRequest = createImagePullSecretRequest
        )
        logger.info("createReleaseNsImagePullSecret secretName:$secretName result is:$createReleaseNsImagePullSecretResult")
        // 创建已发布扩展服务版本的命名空间拉取镜像secret
        val graySecretName = extServiceImageSecretConfig.graySecretName
        val createGrayNsImagePullSecretResult = client.get(ServiceBcsResource::class).createImagePullSecretTest(
            namespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName,
            secretName = graySecretName,
            createImagePullSecretRequest = createImagePullSecretRequest
        )
        logger.info("createGrayNsImagePullSecretResult secretName:$graySecretName result is:$createGrayNsImagePullSecretResult")
        logger.info("end execute initBcsImagePullSecret")
    }
}
