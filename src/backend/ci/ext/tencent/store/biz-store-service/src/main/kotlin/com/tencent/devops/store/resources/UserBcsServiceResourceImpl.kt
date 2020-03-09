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

package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserBcsServiceResource
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue
import io.fabric8.kubernetes.api.model.extensions.IngressBackend
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder
import io.fabric8.kubernetes.api.model.extensions.IngressRule
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.VersionInfo
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.UnsupportedEncodingException
import java.util.Collections

@RestResource
class UserBcsServiceResourceImpl @Autowired constructor() : UserBcsServiceResource {

    private val logger = LoggerFactory.getLogger(UserBcsServiceResourceImpl::class.java)

    override fun bcsVersionTest(userId: String): Result<VersionInfo> {
        val config = ConfigBuilder()
            .withMasterUrl("https://bcs.ied.com:30443/tunnels/clusters/bcs-bcs-k8s-40089-voucrpgo-eitbwFRzIgOZsxQs")
            .withTrustCerts(true)
            .withOauthToken("GPyspRYTYkhXpttRPWWAraHXGXnXZreq")
            .build()
        val client: KubernetesClient = DefaultKubernetesClient(config)
        val versionInfo = client.version
        logger.info("the versionInfo is:$versionInfo")
        return Result(versionInfo)
    }

    override fun bcsDeployTest(userId: String): Result<Boolean> {
        val config = ConfigBuilder()
            .withMasterUrl("https://bcs.ied.com:30443/tunnels/clusters/bcs-bcs-k8s-40089-voucrpgo-eitbwFRzIgOZsxQs")
            .withTrustCerts(true)
            .withOauthToken("GPyspRYTYkhXpttRPWWAraHXGXnXZreq")
            .build()
        val client: KubernetesClient = DefaultKubernetesClient(config)
        val testNameSpaceName = "ext-service-test"
        var ns = client.namespaces().withName(testNameSpaceName).get()
        logger.info("the namespace is: $ns")
        if (ns == null) {
            ns =
                NamespaceBuilder().withNewMetadata().withName(testNameSpaceName).addToLabels("this", "rocks").endMetadata()
                    .build()
            logger.info("Created namespace", client.namespaces().createOrReplace(ns))
        }
        val registryUrl = "docker.dev.bkrepo.oa.com"
        val name = "bk_extension"
        val email = "devops@tencent.com"
        val password = "blueking"
        val secretName = "-bk-docker-secret"
        var secret = client.secrets().inNamespace(testNameSpaceName).withName(testNameSpaceName + secretName).get()
        logger.info("the secret is: $secret")
        if (secret == null) {
            try {
                val secretData: HashMap<String, String> = HashMap(1)
                val basicAuth = String(Base64.encodeBase64("$name:$password".toByteArray()))
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
                    registryUrl,
                    name,
                    password,
                    email,
                    basicAuth
                )
                logger.info("dockerCfg--before-->:$dockerCfg")
                dockerCfg = String(Base64.encodeBase64(dockerCfg.toByteArray(charset("UTF-8"))), Charsets.UTF_8)
                logger.info("dockerCfg--after-->:$dockerCfg")
                secretData[".dockerconfigjson"] = dockerCfg
                val secretBuilder = SecretBuilder()
                secretBuilder
                    .withNewMetadata()
                    .withName(testNameSpaceName + secretName)
                    .withNamespace(testNameSpaceName)
                    .endMetadata()
                    .withData(secretData)
                    .withType("kubernetes.io/dockerconfigjson")
                secret = client.secrets().inNamespace(testNameSpaceName).create(secretBuilder.build())
                logger.info("Creating new secret: $secret")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        val deploymentName = "ext-service-demo2"
        var deployment = DeploymentBuilder()
            .withApiVersion("apps/v1")
            .withNewMetadata()
            .withName(deploymentName)
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", deploymentName)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(deploymentName)
            .withImage("docker.dev.bkrepo.oa.com/bk-extension/docker-local/ext-service-demo:1.0.12")
            .addNewPort()
            .withContainerPort(8080)
            .endPort()
            .endContainer()
            .addNewImagePullSecret()
            .withName(testNameSpaceName + secretName)
            .endImagePullSecret()
            .endSpec()
            .endTemplate()
            .withNewSelector()
            .addToMatchLabels("app", deploymentName)
            .endSelector()
            .endSpec()
            .build()


        deployment = client.apps().deployments().inNamespace(testNameSpaceName).create(deployment)
        logger.info("Created deployment:$deployment")
        logger.info("Created replica sets:${client.apps().replicaSets().inNamespace(testNameSpaceName).list().items}")

        // 创建service
        val serviceName = "ingress-service2"
        var service = ServiceBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName(serviceName)
            .endMetadata()
            .withNewSpec()
            .withSelector(Collections.singletonMap("app", deploymentName))
            .addNewPort()
            .withName("test-port")
            .withProtocol("TCP")
            .withPort(8081)
            .withTargetPort(IntOrString(8080))
            .endPort()
            .withType("NodePort")
            .endSpec()
            .build()

        service = client.services().inNamespace(testNameSpaceName).create(service)
        logger.info("Created service with name:${service.getMetadata().getName()} ")

        // 创建ingress
        var ingress = client.extensions().ingresses().inNamespace(testNameSpaceName).withName("test-ingress").get()
        logger.info("the ingress is: $ingress")
        if(ingress == null) {
            val annotationMap = mapOf(
                "kubernetes.io/ingress.class" to "qcloud",
                "kubernetes.io/ingress.subnetId" to "subnet-4xew4yji"
            )
            //generate ingress backend
            val ingressBackend: IngressBackend = IngressBackendBuilder()
                .withServiceName("ingress-service")
                .withNewServicePort(8081)
                .build()
            //generate ingress path
            val ingressPath = HTTPIngressPathBuilder()
                .withBackend(ingressBackend)
                .withPath("/").build()
            ingress = IngressBuilder()
                .withApiVersion("extensions/v1beta1")
                .withNewMetadata()
                .withName("test-ingress")
                .withNamespace(testNameSpaceName)
                .addToLabels("from", "bkdevops")
                .addToAnnotations(annotationMap)
                .endMetadata()
                .withNewSpec()
                .addNewRule()
                .withHost("demo.ingress.devops.oa.com")
                .withNewHttp()
                .withPaths(ingressPath)
                .endHttp()
                .endRule()
                .endSpec()
                .build()
            ingress = client.extensions().ingresses().inNamespace(testNameSpaceName).create(ingress)
            logger.info("Created ingress with name:${ingress.getMetadata().getName()} ")
        }
        val ingressBackend: IngressBackend = IngressBackendBuilder()
            .withServiceName("ingress-service2")
            .withNewServicePort(8081)
            .build()
        //generate ingress path
        val ingressPath1 = HTTPIngressPathBuilder()
            .withBackend(ingressBackend)
            .withPath("/").build()
        val ingressRule1 = IngressRule(
            "demo2.ingress.devops.oa.com",
            HTTPIngressRuleValue(
                listOf(ingressPath1)
            )
        )
        ingress.spec.rules.add(ingressRule1)
        client.extensions().ingresses().inNamespace(testNameSpaceName).createOrReplace(ingress)
        return Result(true)
    }
}