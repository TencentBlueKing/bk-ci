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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
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
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus
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

    @Autowired
    private lateinit var storeMemberDao: StoreMemberDao

    fun generateDeployApp(
        namespaceName: String,
        serviceCode: String,
        version: String
    ): DeployApp {
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val grayFlag = namespaceName == extServiceBcsNameSpaceConfig.grayNamespaceName
        val host = if (grayFlag) extServiceIngressConfig.grayHost else extServiceIngressConfig.host
        return DeployApp(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            namespaceName = namespaceName,
            appCode = serviceCode,
            appDeployment = AppDeployment(
                replicas = extServiceDeploymentConfig.replicas.toInt(),
                image = "${extServiceImageSecretConfig.repoRegistryUrl}/$imageName:$version",
                pullImageSecretName = if (grayFlag) extServiceDeploymentConfig.grayPullImageSecretName else extServiceDeploymentConfig.pullImageSecretName,
                containerPort = extServiceDeploymentConfig.containerPort.toInt()
            ),
            appService = AppService(
                servicePort = extServiceServiceConfig.servicePort.toInt()
            ),
            appIngress = AppIngress(
                host = MessageFormat(host).format(arrayOf(serviceCode)),
                contextPath = extServiceIngressConfig.contextPath,
                ingressAnnotationMap = mapOf(
                    "kubernetes.io/ingress.class" to extServiceIngressConfig.annotationClass,
                    "kubernetes.io/ingress.existLbId" to if (grayFlag) extServiceIngressConfig.annotationGrayExistLbId else extServiceIngressConfig.annotationExistLbId
                )
            ),
            deployTimeOut = extServiceBcsConfig.deployTimeOut.toInt()
        )
    }

    /**
     * 部署扩展服务应用
     * @param userId 用户ID
     * @param grayFlag 是否是灰度标识
     * @param serviceCode 扩展服务代码
     * @param version 扩展服务版本号
     */
    fun deployExtService(
        userId: String,
        grayFlag: Boolean,
        serviceCode: String,
        version: String
    ): Result<Boolean> {
        logger.info("deployExtService userId is:$userId,grayFlag is:$grayFlag")
        logger.info("deployExtService serviceCode is:$serviceCode,version is:$version")
        if (!storeMemberDao.isStoreMember(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val namespaceName = if (!grayFlag) extServiceBcsNameSpaceConfig.namespaceName else extServiceBcsNameSpaceConfig.grayNamespaceName
        val deployApp = generateDeployApp(namespaceName, serviceCode, version)
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
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        // 停止扩展服务部署（灰度命名空间和正式命名空间的扩展服务应用都需停止）
        val bcsStopAppResult = client.get(ServiceBcsResource::class).bcsStopApp(
            userId = userId,
            stopApp = StopApp(
                bcsUrl = extServiceBcsConfig.masterUrl,
                token = extServiceBcsConfig.token,
                grayNamespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName,
                grayHost = extServiceIngressConfig.grayHost,
                namespaceName = extServiceBcsNameSpaceConfig.namespaceName,
                host = extServiceIngressConfig.host,
                deploymentName = serviceCode,
                serviceName = "$serviceCode-service"
            )
        )
        logger.info("the bcsStopAppResult is :$bcsStopAppResult")
        return bcsStopAppResult
    }

    fun getExtServiceDeployStatus(
        userId: String,
        serviceCode: String,
        grayFlag: Boolean?
    ): Result<DeploymentStatus?> {
        logger.info("getExtServiceDeployStatus userId is:$userId,serviceCode is:$serviceCode,grayFlag is:$grayFlag")
        // 判断用户是否有权限查询部署状态
        if (!storeMemberDao.isStoreMember(dslContext, userId, serviceCode, StoreTypeEnum.SERVICE.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val deployment = client.get(ServiceBcsResource::class).getBcsDeploymentInfo(
            namespaceName = if (grayFlag == null || !grayFlag) extServiceBcsNameSpaceConfig.namespaceName else extServiceBcsNameSpaceConfig.grayNamespaceName,
            deploymentName = serviceCode,
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token
        ).data
        logger.info("getExtServiceDeployStatus deployment is:$deployment")
        return Result(deployment?.status)
    }
}