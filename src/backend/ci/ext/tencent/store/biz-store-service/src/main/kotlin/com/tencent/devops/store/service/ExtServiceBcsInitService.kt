package com.tencent.devops.store.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.api.ServiceBcsResource
import com.tencent.devops.dispatch.pojo.CreateBcsNameSpaceRequest
import com.tencent.devops.dispatch.pojo.CreateImagePullSecretRequest
import com.tencent.devops.dispatch.pojo.KubernetesLabel
import com.tencent.devops.dispatch.pojo.KubernetesRepo
import com.tencent.devops.store.config.ExtServiceBcsConfig
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ExtServiceBcsInitService @Autowired constructor(
    private val client: Client,
    private val extServiceBcsConfig: ExtServiceBcsConfig,
    private val extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig,
    private val extServiceImageSecretConfig: ExtServiceImageSecretConfig
) {

    private val logger = LoggerFactory.getLogger(ExtServiceBcsInitService::class.java)

    @PostConstruct
    fun initBcsNamespace() {
        logger.info("begin execute initBcsNamespace")
        logger.info("extServiceBcsConfig is:$extServiceBcsConfig")
        // 初始化bcs命名空间（包括已发布扩展服务版本的命名空间和处于测试中扩展服务版本的命名空间）
        val namespaceName = extServiceBcsNameSpaceConfig.namespaceName
        val createBcsNameSpaceRequest = CreateBcsNameSpaceRequest(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            kubernetesLabel = KubernetesLabel(
                labelKey = extServiceBcsNameSpaceConfig.labelKey,
                labelValue = extServiceBcsNameSpaceConfig.labelValue
            )
        )
        // 创建已发布扩展服务版本的命名空间
        val releaseNamespaceResult =
            client.get(ServiceBcsResource::class).createNamespace(namespaceName, createBcsNameSpaceRequest)
        logger.info("create namespace:$namespaceName result is:$releaseNamespaceResult")
        val prepareNamespaceName = "$namespaceName-prepare"
        // 创建测试中扩展服务版本的命名空间
        val prepareNamespaceResult =
            client.get(ServiceBcsResource::class).createNamespace(prepareNamespaceName, createBcsNameSpaceRequest)
        logger.info("create namespace:$prepareNamespaceName result is:$prepareNamespaceResult")
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
        // 创建拉取镜像secret
        val createImagePullSecretResult = client.get(ServiceBcsResource::class).createImagePullSecretTest(
            namespaceName = extServiceBcsNameSpaceConfig.namespaceName,
            secretName = secretName,
            createImagePullSecretRequest = createImagePullSecretRequest
        )
        logger.info("create secretName:$secretName result is:$createImagePullSecretResult")
        logger.info("end execute initBcsImagePullSecret")
    }
}