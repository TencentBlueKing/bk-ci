/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.pojo.VMCreate
import com.tencent.devops.dispatch.pojo.VMWithPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Created by rdeng on 2017/9/4.
 */
@Api(tags = ["OP_VM"], description = "VM 管理")
@Path("/op/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpVMResource {

    @ApiOperation("获取所有的VM虚拟机信息")
    @GET
    @Path("/")
    fun list(
        @ApiParam(value = "IP", required = false)
        @QueryParam(value = "ip")
        ip: String?,
        @ApiParam(value = "主机名", required = false)
        @QueryParam(value = "name")
        name: String?,
        @ApiParam(value = "类型ID", required = false)
        @QueryParam(value = "typeId")
        typeId: Int?,
        @ApiParam(value = "操作系统", required = false)
        @QueryParam(value = "os")
        os: String?,
        @ApiParam(value = "操作系统版本", required = false)
        @QueryParam(value = "osVersion")
        osVersion: String?,
        @ApiParam(value = "分页Offset", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam(value = "分页数量", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<VMWithPage>

    @ApiOperation("根据虚拟机ID获取虚拟机详情")
    @GET
    @Path("/{vmId}")
    fun get(
        @ApiParam(value = "虚拟机ID", required = true)
        @PathParam("vmId")
        vmId: Int
    ): Result<VM>

    @ApiOperation("虚拟机入库")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "VM 信息", required = true)
        vm: VMCreate
    ): Result<Boolean>

    @ApiOperation("根据IP获得对应的虚拟机信息")
    @GET
    @Path("/listByIp")
    fun getByIp(
        @ApiParam(value = "VM IP", required = true)
        @QueryParam("ip") ip: String
    ): Result<VM>

    @ApiOperation("删除虚拟机")
    @DELETE
    @Path("/")
    fun delete(
        @ApiParam(value = "VM ID", required = true)
        @QueryParam("id") id: Int
    ): Result<Boolean>

    @ApiOperation("更新虚拟机")
    @PUT
    @Path("/")
    fun update(
        @ApiParam(value = "VM 信息", required = true)
        vm: VMCreate
    ): Result<Boolean>

    @ApiOperation("查询VM状态")
    @GET
    @Path("/status")
    fun queryStatus(
        @ApiParam(value = "VM 名称", required = true)
        @QueryParam("vmName") vmName: String
    ): Result<String>

    @ApiOperation("虚拟机进入或者解除维护状态")
    @PUT
    @Path("/maintain")
    fun maintain(
        @ApiParam(value = "VM ID", required = true)
        @QueryParam("vmId") vmId: Int,
        @ApiParam(value = "Enable", required = true)
        @QueryParam("enable") enable: Boolean
    ): Result<Boolean>

    @ApiOperation("虚拟机进入或者解除维护状态")
    @GET
    @Path("/maintain")
    fun maintain(
        @ApiParam(value = "VM ID", required = true)
        @QueryParam("vmId") vmId: Int
    ): Result<Boolean>

    @ApiOperation("If need to shutdown the vm after the build (currently just for investigating issues")
    @PUT
    @Path("/shutdownAfterBuild")
    fun shutdownAfterBuild(
        @ApiParam(value = "shutdown or now", required = true)
        @QueryParam("shutdown")
        shutdown: Boolean,
        @ApiParam(value = "pipeline ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @ApiOperation("If need to shutdown the vm after the build (currently just for investigating issues")
    @GET
    @Path("/shutdownAfterBuild")
    fun isShutdownAfterBuild(): Result<String?>
}
