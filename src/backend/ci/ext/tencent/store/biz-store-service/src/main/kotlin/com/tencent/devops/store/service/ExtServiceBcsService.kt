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
import com.tencent.devops.store.pojo.dto.DeployExtServiceDTO
import com.tencent.devops.store.resources.UserBcsServiceResourceImpl
import com.tencent.devops.store.util.BcsClientUtils
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.ContainerPort
import io.fabric8.kubernetes.api.model.LocalObjectReference
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.PodSpec
import io.fabric8.kubernetes.api.model.PodTemplateSpec
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceBcsService @Autowired constructor() {

    private val logger = LoggerFactory.getLogger(UserBcsServiceResourceImpl::class.java)

    fun deployExtService(
        userId: String,
        deployExtServiceDTO: DeployExtServiceDTO
    ): Result<Boolean> {
        logger.info("deployExtService userId is: $userId,deployExtServiceDTO is: $deployExtServiceDTO")
        val serviceCode = deployExtServiceDTO.serviceCode
        val deployment = Deployment()
        val metadata = ObjectMeta()
        metadata.name = serviceCode
        metadata.namespace = deployExtServiceDTO.namespaceName
        deployment.metadata = metadata
        val deploymentSpec = DeploymentSpec()
        deploymentSpec.replicas = deployExtServiceDTO.replicas
        val podTemplateSpec = PodTemplateSpec()
        val specMetadata = ObjectMeta()
        specMetadata.labels = mapOf("app" to serviceCode)
        podTemplateSpec.metadata = specMetadata
        val podSpec = PodSpec()
        val container = Container()
        container.name = serviceCode
        container.image = deployExtServiceDTO.serviceImage
        val containerPortList = arrayListOf<ContainerPort>()
        deployExtServiceDTO.ports.forEach {
            val containerPort = ContainerPort()
            containerPort.hostPort = it
            containerPortList.add(containerPort)
        }
        container.ports = containerPortList
        podSpec.containers = listOf(container)
        if (deployExtServiceDTO.pullImageSecretName != null) {
            val localObjectReference = LocalObjectReference(deployExtServiceDTO.pullImageSecretName)
            podSpec.imagePullSecrets = listOf(localObjectReference)
        }
        podTemplateSpec.spec = podSpec
        deploymentSpec.template = podTemplateSpec
        BcsClientUtils.createDeployment(deployment)
        return Result(true)
    }
}