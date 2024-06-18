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

package com.tencent.devops.dispatch.kubernetes.bcs.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.dispatch.kubernetes.bcs.config.KubernetesConfig
import com.tencent.devops.dispatch.kubernetes.client.SecretClient
import com.tencent.devops.dispatch.kubernetes.pojo.base.KubernetesRepo
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service

@Service
@DependsOn("springContextUtil")
class BcsInitService @Autowired constructor(
    private val secretClient: SecretClient,
    private val kubernetesConfig: KubernetesConfig
) {

    private val logger = LoggerFactory.getLogger(BcsInitService::class.java)

    @PostConstruct
    fun initBcsImagePullSecret() {
        logger.info("begin execute initBcsImagePullSecret")
        try {
            val kubernetesRepo = KubernetesRepo(
                registryUrl = kubernetesConfig.repoRegistryUrl,
                username = kubernetesConfig.repoUsername,
                password = kubernetesConfig.repoPassword,
                email = kubernetesConfig.repoEmail
            )
            // 创建已发布扩展服务版本的命名空间拉取镜像secret
            secretClient.createImagePullSecret(
                userId = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
                namespaceName = kubernetesConfig.namespaceName,
                secretName = kubernetesConfig.secretName,
                kubernetesRepoInfo = kubernetesRepo
            )
            // 创建已发布扩展服务版本的命名空间拉取镜像secret
            secretClient.createImagePullSecret(
                userId = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
                namespaceName = kubernetesConfig.grayNamespaceName,
                secretName = kubernetesConfig.graySecretName,
                kubernetesRepoInfo = kubernetesRepo
            )
        } catch (ignored: Throwable) {
            logger.warn("init bcs image pull secret fail, case:${ignored.message}")
        }
        logger.info("end execute initBcsImagePullSecret")
    }
}
