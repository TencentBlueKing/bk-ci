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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmReq
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.image.StandardVmImage
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_DISPATCH_KUBERNETES_REMOTE_DEV", description = "START云桌面接口模块")
@Path("/service/startCloud")
@ServiceInterface("dispatch") // 指明接入到哪个微服务
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStartCloudResource {
    @Operation(summary = "创建START云桌面用户")
    @GET
    @Path("/startCloud/user/create")
    fun createStartCloudUser(
        @Parameter(description = "user", required = true)
        @QueryParam("user")
        user: String
    ): Result<Boolean>

    @Operation(summary = "同步更新START云桌面的资源池")
    @GET
    @Path("/startCloud/resourece/list")
    fun syncStartCloudResourceList(): Result<List<EnvironmentResourceData>>

    @Operation(summary = "根据cgsId获取资源信息")
    @POST
    @Path("/startCloud/cgs")
    fun getCgsData(
        @Parameter(description = "查询数据")
        data: FetchWinPoolData
    ): Result<List<EnvironmentResourceData>>

    @Operation(summary = "根据cgsId确认是否云桌面已有使用中的记录")
    @GET
    @Path("/workspace/check")
    fun checkCgsRunning(
        @Parameter(description = "cgsId", required = true)
        @QueryParam("cgsId")
        cgsId: String,
        @Parameter(description = "status", required = true)
        @QueryParam("status")
        status: EnvStatusEnum? = EnvStatusEnum.running
    ): Result<Boolean>

    @Operation(summary = "获取CGS资源池的区域和机型列表")
    @GET
    @Path("/windows/pool/config")
    fun getCgsConfig(): Result<CgsResourceConfig>

    @Operation(summary = "根据cgsId确认是否云桌面已有使用中的记录")
    @POST
    @Path("/workspace/share")
    fun shareWorkspace(
        @Parameter(description = "operator", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "cgsId", required = false)
        @QueryParam("cgsId")
        cgsId: String,
        receivers: List<String>
    ): Result<String>

    @Operation(summary = "根据cgsId确认是否云桌面已有使用中的记录")
    @POST
    @Path("/workspace/unShare")
    fun unShareWorkspace(
        @Parameter(description = "operator", required = true)
        @QueryParam("operator")
        operator: String,
        @Parameter(description = "resourceId", required = true)
        @QueryParam("resourceId")
        resourceId: String,
        receivers: List<String>
    ): Result<Boolean>

    @Operation(summary = "查询vm集群资源数")
    @POST
    @Path("/resource/vm")
    fun getResourceVm(
        data: ResourceVmReq
    ): Result<List<ResourceVmRespData>?>

    @Operation(summary = "根据eid拿到对应vm信息")
    @GET
    @Path("/workspace/eid")
    fun getWorkspaceInfoByEid(
        @Parameter(description = "uid", required = true)
        @QueryParam("eid")
        eid: String
    ): Result<WorkspaceInfo>

    @Operation(summary = "根据uid拿到对应task回调信息")
    @GET
    @Path("/workspace/uid")
    fun getTaskInfoByUid(
        @Parameter(description = "uid", required = true)
        @QueryParam("uid")
        uid: String
    ): Result<TaskStatus?>

    @Operation(summary = "查询VM基础镜像")
    @POST
    @Path("/resource/standard/image")
    fun getVmStandardImages(): Result<List<StandardVmImage>?>
}
