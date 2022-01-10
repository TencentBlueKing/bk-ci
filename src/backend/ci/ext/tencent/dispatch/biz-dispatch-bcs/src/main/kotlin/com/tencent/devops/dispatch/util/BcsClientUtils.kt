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

package com.tencent.devops.dispatch.util

import com.tencent.devops.dispatch.pojo.KubernetesLabel
import com.tencent.devops.dispatch.pojo.KubernetesLimitRange
import com.tencent.devops.dispatch.pojo.KubernetesRepo
import io.fabric8.kubernetes.api.model.LimitRangeBuilder
import io.fabric8.kubernetes.api.model.LimitRangeItem
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Quantity
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
import java.util.concurrent.ConcurrentHashMap

@SuppressWarnings("ALL")
object BcsClientUtils {

    private val logger = LoggerFactory.getLogger(BcsClientUtils::class.java)

    private const val cpu = "cpu"

    private const val memory = "memory"

    private val bcsKubernetesClientMap = ConcurrentHashMap<String, KubernetesClient>()

    fun getBcsKubernetesClientMap(): ConcurrentHashMap<String, KubernetesClient> {
        return bcsKubernetesClientMap
    }

    private fun createBcsKubernetesClient(
        bcsUrl: String,
        token: String
    ): KubernetesClient {
        val config = ConfigBuilder()
            .withMasterUrl(bcsUrl)
            .withTrustCerts(true)
            .withOauthToken(token)
            .build()
        return DefaultKubernetesClient(config)
    }

    fun getBcsKubernetesClient(
        bcsUrl: String,
        token: String
    ): KubernetesClient {
        val bcsKubernetesClientKey = "$bcsUrl;$token"
        var bcsKubernetesClient = bcsKubernetesClientMap[bcsKubernetesClientKey]
        if (bcsKubernetesClient == null) {
            // 删除缓存中bcUrl相同但token因为过期不相同的记录
            bcsKubernetesClientMap.entries.removeIf { entry -> entry.key.startsWith("$bcsUrl;") }
            bcsKubernetesClient = createBcsKubernetesClient(bcsUrl, token)
            bcsKubernetesClientMap[bcsKubernetesClientKey] = bcsKubernetesClient
        }
        return bcsKubernetesClient
    }

    /**
     * 创建k8s命名空间
     * @param bcsUrl bcs接口路径
     * @param token token
     * @param namespaceName 命名空间名称
     * @param labelInfo 标签信息
     * @param limitRangeInfo k8s资源限制信息
     */
    fun createNamespace(
        bcsUrl: String,
        token: String,
        namespaceName: String,
        labelInfo: KubernetesLabel,
        limitRangeInfo: KubernetesLimitRange? = null
    ): Namespace {
        logger.info("createNamespace namespaceName is: $namespaceName,labelInfo is: $labelInfo,limitRangeInfo is: $limitRangeInfo")
        val bcsKubernetesClient = getBcsKubernetesClient(bcsUrl, token)
        var ns = bcsKubernetesClient.namespaces().withName(namespaceName).get()
        logger.info("the namespace is: $ns")
        if (ns == null) {
            ns =
                NamespaceBuilder().withNewMetadata().withName(namespaceName)
                    .addToLabels(labelInfo.labelKey, labelInfo.labelValue).endMetadata()
                    .build()
            val namespace = bcsKubernetesClient.namespaces().createOrReplace(ns)
            logger.info("created namespace:$namespace")
        }
        val limitRangeItem = LimitRangeItem()
        if (null != limitRangeInfo) {
            limitRangeItem.default = mapOf(cpu to Quantity(limitRangeInfo.defaultCpu), memory to Quantity(limitRangeInfo.defaultMemory))
            limitRangeItem.defaultRequest = mapOf(cpu to Quantity(limitRangeInfo.defaultRequestCpu), memory to Quantity(limitRangeInfo.defaultRequestMemory))
            limitRangeItem.type = limitRangeInfo.limitType
            val limitRange = LimitRangeBuilder().withNewMetadata().withName("$namespaceName-limit")
                .endMetadata().withNewSpec().addToLimits(limitRangeItem).endSpec().build()
            bcsKubernetesClient.limitRanges().inNamespace(namespaceName).createOrReplace(limitRange)
        }
        return ns
    }

    /**
     * 创建k8s拉取镜像secret
     * @param bcsUrl bcs接口路径
     * @param token token
     * @param namespaceName 命名空间名称
     * @param secretName 秘钥名称
     * @param kubernetesRepoInfo k8s仓库信息
     */
    fun createImagePullSecret(
        bcsUrl: String,
        token: String,
        secretName: String,
        namespaceName: String,
        kubernetesRepoInfo: KubernetesRepo
    ): Secret {
        logger.info("createImagePullSecret secretName is: $secretName,namespaceName is: $namespaceName")
        val bcsKubernetesClient = getBcsKubernetesClient(bcsUrl, token)
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
            secret = bcsKubernetesClient.secrets().inNamespace(namespaceName).createOrReplace(secretBuilder.build())
            logger.info("create new secret: $secret")
        }
        return secret
    }

    /**
     * 创建deployment
     * @param bcsUrl bcs接口路径
     * @param token token
     * @param namespaceName 命名空间名称
     * @param deployment 无状态部署对象
     */
    fun createDeployment(
        bcsUrl: String,
        token: String,
        namespaceName: String,
        deployment: Deployment
    ): Deployment {
        val bcsKubernetesClient = getBcsKubernetesClient(bcsUrl, token)
        return bcsKubernetesClient.apps().deployments().inNamespace(namespaceName).createOrReplace(deployment)
    }

    /**
     * 创建service
     * @param bcsUrl bcs接口路径
     * @param token token
     * @param namespaceName 命名空间名称
     * @param service service对象
     */
    fun createService(
        bcsUrl: String,
        token: String,
        namespaceName: String,
        service: Service
    ): Service {
        val bcsKubernetesClient = getBcsKubernetesClient(bcsUrl, token)
        return bcsKubernetesClient.services().inNamespace(namespaceName).createOrReplace(service)
    }

    /**
     * 创建ingress
     * @param bcsUrl bcs接口路径
     * @param token token
     * @param namespaceName 命名空间名称
     * @param ingress ingress对象
     */
    fun createIngress(
        bcsUrl: String,
        token: String,
        namespaceName: String,
        ingress: Ingress
    ): Ingress {
        val bcsKubernetesClient = getBcsKubernetesClient(bcsUrl, token)
        return bcsKubernetesClient.extensions().ingresses().inNamespace(namespaceName).createOrReplace(ingress)
    }
}
