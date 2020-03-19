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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.api.ServiceBcsResource
import com.tencent.devops.dispatch.pojo.AppDeployment
import com.tencent.devops.dispatch.pojo.AppIngress
import com.tencent.devops.dispatch.pojo.AppService
import com.tencent.devops.dispatch.pojo.DeployApp
import com.tencent.devops.dispatch.pojo.StopApp
import com.tencent.devops.store.config.ExtServiceBcsConfig
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceDeploymentConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import com.tencent.devops.store.config.ExtServiceIngressConfig
import com.tencent.devops.store.config.ExtServiceServiceConfig
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.MessageFormat

@Service
class ExtServiceBcsService {

    private val logger = LoggerFactory.getLogger(ExtServiceBcsService::class.java)

    @Autowired
    private lateinit var client: Client

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    private lateinit var extServiceBcsConfig: ExtServiceBcsConfig

    @Autowired
    private lateinit var extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig

    @Autowired
    private lateinit var extServiceImageSecretConfig: ExtServiceImageSecretConfig

    @Autowired
    private lateinit var extServiceDeploymentConfig: ExtServiceDeploymentConfig

    @Autowired
    private lateinit var extServiceServiceConfig: ExtServiceServiceConfig

    @Autowired
    private lateinit var extServiceIngressConfig: ExtServiceIngressConfig

    fun generateDeployApp(
        namespaceName: String,
        serviceCode: String,
        version: String,
        grayFlag: Boolean
    ): DeployApp {
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val hostPrefix = if (grayFlag) "$serviceCode-gray" else serviceCode
        return DeployApp(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            namespaceName = namespaceName,
            appCode = serviceCode,
            appDeployment = AppDeployment(
                replicas = extServiceDeploymentConfig.replicas.toInt(),
                image = "${extServiceImageSecretConfig.repoRegistryUrl}/$imageName:$version",
                pullImageSecretName = extServiceDeploymentConfig.grayPullImageSecretName,
                containerPort = extServiceDeploymentConfig.containerPort.toInt()
            ),
            appService = AppService(
                servicePort = extServiceServiceConfig.servicePort.toInt()
            ),
            appIngress = AppIngress(
                host = MessageFormat(extServiceIngressConfig.host).format(arrayOf(hostPrefix)),
                contextPath = extServiceIngressConfig.contextPath,
                ingressAnnotationMap = mapOf(
                    "kubernetes.io/ingress.class" to extServiceIngressConfig.annotationClass,
                    "kubernetes.io/ingress.subnetId" to extServiceIngressConfig.annotationSubnetId
                )
            )
        )
    }

    /**
     * 部署扩展服务应用
     * @param userId 用户ID
     * @param namespaceName 命名空间名称
     * @param serviceCode 扩展服务代码
     * @param version 扩展服务版本号
     */
    fun deployExtService(
        userId: String,
        namespaceName: String,
        serviceCode: String,
        version: String,
        grayFlag: Boolean
    ): Result<Boolean> {
        logger.info("deployExtService userId is:$userId,namespaceName is:$namespaceName")
        logger.info("deployExtService serviceCode is:$serviceCode,version is:$version,grayFlag is:$grayFlag")
        val deployApp = generateDeployApp(namespaceName, serviceCode, version, grayFlag)
        val bcsDeployAppResult = client.get(ServiceBcsResource::class).bcsDeployApp(
            userId = userId,
            deployApp = deployApp
        )
        logger.info("bcsDeployAppResult is :$bcsDeployAppResult")
        return bcsDeployAppResult
    }

    /**
     * 停止扩展服务应用
     * @param userId 用户ID
     * @param serviceCode 扩展服务代码
     * @param deploymentName deployment名称
     * @param serviceName service名称
     */
    fun stopExtService(
        userId: String,
        serviceCode: String,
        deploymentName: String,
        serviceName: String
    ): Result<Boolean> {
        logger.info("stopExtService userId is:$userId,serviceCode is:$serviceCode")
        logger.info("stopExtService deploymentName is:$deploymentName,serviceName is:$serviceName")
        // 停止扩展服务部署（灰度命名空间和正式命名空间的扩展服务应用都需停止）
        val bcsStopAppResult = client.get(ServiceBcsResource::class).bcsStopApp(
            userId = userId,
            stopApp = StopApp(
                bcsUrl = extServiceBcsConfig.masterUrl,
                token = extServiceBcsConfig.token,
                grayNamespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName,
                namespaceName = extServiceBcsNameSpaceConfig.namespaceName,
                deploymentName = serviceCode,
                serviceName = "$serviceCode-service"
            )
        )
        logger.info("the bcsStopAppResult is :$bcsStopAppResult")
        return bcsStopAppResult
    }
}