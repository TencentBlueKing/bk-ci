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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.AppIngress
import com.tencent.devops.dispatch.pojo.DeployApp
import com.tencent.devops.dispatch.util.BcsClientUtils
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue
import io.fabric8.kubernetes.api.model.extensions.Ingress
import io.fabric8.kubernetes.api.model.extensions.IngressBackend
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressRule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections

@Service
class BcsDeployService @Autowired constructor(private val redisOperation: RedisOperation) {

    private val logger = LoggerFactory.getLogger(BcsDeployService::class.java)

    private final val defaultLabelKey = "app"

    fun deployApp(
        userId: String,
        deployApp: DeployApp
    ): Result<Boolean> {
        logger.info("deployApp userId is: $userId,app is: $deployApp")
        val bcsUrl = deployApp.bcsUrl
        val token = deployApp.token
        val namespaceName = deployApp.namespaceName
        val serviceCode = deployApp.appCode
        val appDeployment = deployApp.appDeployment
        val appService = deployApp.appService
        val appIngress = deployApp.appIngress
        val containerPort = appDeployment.containerPort
        // 创建deployment无状态部署
        val deployment = DeploymentBuilder()
            .withNewMetadata()
            .withName(serviceCode)
            .endMetadata()
            .withNewSpec()
            .withReplicas(appDeployment.replicas)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels(defaultLabelKey, serviceCode)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(serviceCode)
            .withImage(appDeployment.image)
            .addNewPort()
            .withContainerPort(containerPort)
            .endPort()
            .endContainer()
            .addNewImagePullSecret()
            .withName(appDeployment.pullImageSecretName)
            .endImagePullSecret()
            .endSpec()
            .endTemplate()
            .withNewSelector()
            .addToMatchLabels(defaultLabelKey, serviceCode)
            .endSelector()
            .endSpec()
            .build()
        BcsClientUtils.createDeployment(bcsUrl, token, namespaceName, deployment)
        logger.info("created deployment:$deployment")
        val servicePort = appService.servicePort
        val service = ServiceBuilder()
            .withNewMetadata()
            .withName("$serviceCode-service")
            .endMetadata()
            .withNewSpec()
            .withSelector(Collections.singletonMap(defaultLabelKey, serviceCode))
            .addNewPort()
            .withName("$serviceCode-port")
            .withProtocol("TCP")
            .withPort(servicePort)
            .withTargetPort(IntOrString(containerPort))
            .endPort()
            .withType("NodePort")
            .endSpec()
            .build()
        BcsClientUtils.createService(bcsUrl, token, namespaceName, service)
        logger.info("created service:$service")
        // 创建ingress
        // generate ingress backend
        val ingressBackend: IngressBackend = IngressBackendBuilder()
            .withServiceName("$serviceCode-service")
            .withNewServicePort(servicePort)
            .build()
        // generate ingress path
        val ingressPath = HTTPIngressPathBuilder()
            .withBackend(ingressBackend)
            .withPath(appIngress.contextPath).build()
        val ingressRule = IngressRule(
            appIngress.host,
            HTTPIngressRuleValue(
                listOf(ingressPath)
            )
        )
        val ingressRedisKey = "ext:service:ingress:$namespaceName"
        val ingressName = redisOperation.get(ingressRedisKey)
        logger.info("deployApp ingressName is: $ingressName")
        if (ingressName.isNullOrBlank()) {
            val ingress = createIngress(
                bcsUrl = bcsUrl,
                token = token,
                namespaceName = namespaceName,
                serviceCode = serviceCode,
                appIngress = appIngress,
                ingressRule = ingressRule,
                ingressRedisKey = ingressRedisKey
            )
            logger.info("created ingress:$ingress")
        } else {
            val bcsKubernetesClient = BcsClientUtils.getBcsKubernetesClient(bcsUrl, token)
            var ingress =
                bcsKubernetesClient.extensions().ingresses().inNamespace(namespaceName).withName(ingressName).get()
            logger.info("deployApp ingress is: $ingress")
            if (ingress == null) {
                ingress = createIngress(
                    bcsUrl = bcsUrl,
                    token = token,
                    namespaceName = namespaceName,
                    serviceCode = serviceCode,
                    appIngress = appIngress,
                    ingressRule = ingressRule,
                    ingressRedisKey = ingressRedisKey
                )
                logger.info("created ingress:$ingress")
            } else {
                when {
                    ingress.spec.rules.contains(ingressRule) -> return Result(true)
                    else -> {
                        ingress.spec.rules.add(ingressRule)
                        BcsClientUtils.createIngress(bcsUrl, token, namespaceName, ingress)
                        logger.info("update ingress:$ingressName success")
                    }
                }
            }
        }
        return Result(true)
    }

    private fun createIngress(
        bcsUrl: String,
        token: String,
        namespaceName: String,
        serviceCode: String,
        appIngress: AppIngress,
        ingressRule: IngressRule,
        ingressRedisKey: String
    ): Ingress {
        val ingress = IngressBuilder()
            .withNewMetadata()
            .withName("$namespaceName-ingress")
            .addToLabels(defaultLabelKey, serviceCode)
            .addToAnnotations(appIngress.ingressAnnotationMap)
            .endMetadata()
            .withNewSpec()
            .withRules(ingressRule)
            .endSpec()
            .build()
        BcsClientUtils.createIngress(bcsUrl, token, namespaceName, ingress)
        redisOperation.set(
            key = ingressRedisKey,
            value = "$namespaceName-ingress",
            expiredInSecond = null,
            expired = false
        )
        return ingress
    }
}