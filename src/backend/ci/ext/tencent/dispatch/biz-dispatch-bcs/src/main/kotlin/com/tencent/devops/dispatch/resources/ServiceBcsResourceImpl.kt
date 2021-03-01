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

package com.tencent.devops.dispatch.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ServiceBcsResource
import com.tencent.devops.dispatch.pojo.CreateBcsNameSpaceRequest
import com.tencent.devops.dispatch.pojo.CreateImagePullSecretRequest
import com.tencent.devops.dispatch.pojo.DeployApp
import com.tencent.devops.dispatch.pojo.StopApp
import com.tencent.devops.dispatch.service.BcsDeployService
import com.tencent.devops.dispatch.service.BcsQueryService
import com.tencent.devops.dispatch.util.BcsClientUtils
import io.fabric8.kubernetes.api.model.apps.Deployment
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBcsResourceImpl @Autowired constructor(
    private val bcsDeployService: BcsDeployService,
    private val bcsQueryService: BcsQueryService
) : ServiceBcsResource {

    override fun createNamespace(
        namespaceName: String,
        createBcsNameSpaceRequest: CreateBcsNameSpaceRequest
    ): Result<Boolean> {
        BcsClientUtils.createNamespace(
            bcsUrl = createBcsNameSpaceRequest.bcsUrl,
            token = createBcsNameSpaceRequest.token,
            namespaceName = namespaceName,
            labelInfo = createBcsNameSpaceRequest.kubernetesLabel,
            limitRangeInfo = createBcsNameSpaceRequest.limitRangeInfo
        )
        return Result(true)
    }

    override fun createImagePullSecretTest(
        namespaceName: String,
        secretName: String,
        createImagePullSecretRequest: CreateImagePullSecretRequest
    ): Result<Boolean> {
        BcsClientUtils.createImagePullSecret(
            bcsUrl = createImagePullSecretRequest.bcsUrl,
            token = createImagePullSecretRequest.token,
            secretName = secretName,
            namespaceName = namespaceName,
            kubernetesRepoInfo = createImagePullSecretRequest.kubernetesRepo
        )
        return Result(true)
    }

    override fun bcsDeployApp(userId: String, deployApp: DeployApp): Result<Boolean> {
        return bcsDeployService.deployApp(userId, deployApp)
    }

    override fun bcsStopApp(userId: String, stopApp: StopApp): Result<Boolean> {
        return bcsDeployService.stopApp(userId, stopApp)
    }

    override fun getBcsDeploymentInfo(
        namespaceName: String,
        deploymentName: String,
        bcsUrl: String,
        token: String
    ): Result<Deployment> {
        return bcsQueryService.getBcsDeploymentInfo("", namespaceName, deploymentName, bcsUrl, token)
    }

    override fun getBcsDeploymentInfos(
        namespaceName: String,
        deploymentNames: String,
        bcsUrl: String,
        token: String
    ): Result<Map<String, Deployment>> {
        return bcsQueryService.getBcsDeploymentInfos("", namespaceName, deploymentNames, bcsUrl, token)
    }
}
