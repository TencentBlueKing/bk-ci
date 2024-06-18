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

package com.tencent.devops.dispatch.kubernetes.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.kubernetes.pojo.base.KubernetesRepo
import com.tencent.devops.dispatch.pojo.DeployApp
import com.tencent.devops.dispatch.pojo.StopApp
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_BCS", description = "BCS服务")
@Path("/service/kubernetes/management")
@ServiceInterface("dispatch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceKubernetesManagementResource {

    @Operation(summary = "创建拉取镜像secret")
    @Path("/bcs/namespaces/{namespaceName}/secrets/{secretName}/create")
    @POST
    fun createImagePullSecretTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "命名空间名称")
        @PathParam("namespaceName")
        namespaceName: String,
        @Parameter(description = "命名空间名称")
        @PathParam("secretName")
        secretName: String,
        @Parameter(description = "k8s仓库信息")
        kubernetesRepo: KubernetesRepo
    ): Result<Boolean>

    @Operation(summary = "部署应用")
    @Path("/deploy/app")
    @POST
    fun deployApp(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "部署请求对象")
        deployApp: DeployApp
    ): Result<Boolean>

    @Operation(summary = "停止部署应用")
    @Path("/stop/app")
    @DELETE
    fun stopApp(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "停止部署请求对象")
        stopApp: StopApp
    ): Result<Boolean>

    @Operation(summary = "获取deployment信息")
    @Path("/namespaceNames/{namespaceName}/deployments/{deploymentName}")
    @GET
    fun getDeploymentInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "namespaceName")
        @PathParam("namespaceName")
        namespaceName: String,
        @Parameter(description = "deployment名称")
        @PathParam("deploymentName")
        deploymentName: String
    ): Result<Deployment?>

    @Operation(summary = "获取deployment信息集合")
    @Path("/namespaceNames/{namespaceName}/deployments")
    @GET
    fun getDeploymentInfos(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "namespaceName")
        @PathParam("namespaceName")
        namespaceName: String,
        @Parameter(description = "deployment名称")
        @QueryParam("deploymentNames")
        deploymentNames: String
    ): Result<Map<String, Deployment>>
}
