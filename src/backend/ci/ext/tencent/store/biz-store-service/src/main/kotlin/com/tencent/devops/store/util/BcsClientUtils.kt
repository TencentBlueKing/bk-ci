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

package com.tencent.devops.store.util

import com.tencent.devops.store.pojo.dto.kubernetes.KubernetesLabelInfoDTO
import com.tencent.devops.store.pojo.dto.kubernetes.KubernetesRepoInfoDTO
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.extensions.Ingress
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory

@SuppressWarnings("ALL")
object BcsClientUtils {

    private val logger = LoggerFactory.getLogger(BcsClientUtils::class.java)

    private val bcsKubernetesClient = createBcsKubernetesClient()

    private fun createBcsKubernetesClient(): KubernetesClient {
        val config = ConfigBuilder()
            .withMasterUrl("https://bcs.ied.com:30443/tunnels/clusters/bcs-bcs-k8s-40089-voucrpgo-eitbwFRzIgOZsxQs")
            .withTrustCerts(true)
            .withOauthToken("GPyspRYTYkhXpttRPWWAraHXGXnXZreq")
            .build()
        return DefaultKubernetesClient(config)
    }

    fun getBcsKubernetesClient(): KubernetesClient {
        return bcsKubernetesClient
    }

    /**
     * 创建k8s命名空间
     * @param namespaceName 命名空间名称
     * @param labelInfo 标签信息
     */
    fun createNamespace(
        namespaceName: String,
        labelInfo: KubernetesLabelInfoDTO
    ): Namespace {
        logger.info("createNamespace namespaceName is: $namespaceName,labelInfo is: $labelInfo")
        var ns = bcsKubernetesClient.namespaces().withName(namespaceName).get()
        logger.info("the namespace is: $ns")
        if (ns == null) {
            ns =
                NamespaceBuilder().withNewMetadata().withName(namespaceName)
                    .addToLabels(labelInfo.labelKey, labelInfo.labelValue).endMetadata()
                    .build()
            logger.info("created namespace:${bcsKubernetesClient.namespaces().createOrReplace(ns)}")
        }
        return ns
    }

    /**
     * 创建k8s拉取镜像secret
     * @param namespaceName 命名空间名称
     * @param secretName 秘钥名称
     * @param kubernetesRepoInfo k8s仓库信息
     */
    fun createImagePullSecret(
        secretName: String,
        namespaceName: String,
        kubernetesRepoInfo: KubernetesRepoInfoDTO
    ): Secret {
        logger.info("createImagePullSecret secretName is: $secretName,namespaceName is: $namespaceName")
        var secret = bcsKubernetesClient.secrets().inNamespace(namespaceName).withName(secretName).get()
        logger.info("the secret is: $secret")
        if (secret == null) {
            val secretData: HashMap<String, String> = HashMap(1)
            val username = kubernetesRepoInfo.username
            val password = kubernetesRepoInfo.password
            val basicAuth = String(Base64.encodeBase64("$username:$password".toByteArray()))
            var dockerCfg = String.format(
                "{ " +
                    " \"auths\": { " +
                    "  \"%s\": { " +
                    "   \"username\": \"%s\", " +
                    "   \"password\": \"%s\", " +
                    "   \"email\": \"%s\", " +
                    "   \"auth\": \"%s\" " +
                    "  } " +
                    " } " +
                    "}",
                kubernetesRepoInfo.registryUrl,
                username,
                password,
                kubernetesRepoInfo.email,
                basicAuth
            )
            dockerCfg = String(Base64.encodeBase64(dockerCfg.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
            secretData[".dockerconfigjson"] = dockerCfg
            val secretBuilder = SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespaceName)
                .endMetadata()
                .withData(secretData)
                .withType("kubernetes.io/dockerconfigjson")
            secret = bcsKubernetesClient.secrets().inNamespace(namespaceName).create(secretBuilder.build())
            logger.info("create new secret: $secret")
        }
        return secret
    }

    /**
     * 创建deployment
     * @param namespaceName 命名空间名称
     * @param deployment 无状态部署对象
     */
    fun createDeployment(
        namespaceName: String,
        deployment: Deployment
    ): Deployment {
        return bcsKubernetesClient.apps().deployments().inNamespace(namespaceName).createOrReplace(deployment)
    }

    /**
     * 创建service
     * @param namespaceName 命名空间名称
     * @param service service对象
     */
    fun createService(
        namespaceName: String,
        service: Service
    ): Service {
        return bcsKubernetesClient.services().inNamespace(namespaceName).createOrReplace(service)
    }

    /**
     * 创建ingress
     * @param namespaceName 命名空间名称
     * @param ingress ingress对象
     */
    fun createIngress(
        namespaceName: String,
        ingress: Ingress
    ): Ingress {
        return bcsKubernetesClient.extensions().ingresses().inNamespace(namespaceName).create(ingress)
    }
}