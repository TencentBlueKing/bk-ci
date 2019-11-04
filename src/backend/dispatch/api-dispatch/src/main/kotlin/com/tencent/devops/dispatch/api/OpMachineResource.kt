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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.Machine
import com.tencent.devops.dispatch.pojo.MachineCreate
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

@Api(tags = ["OP_MACHINE"], description = "VM 机器管理")
@Path("/op/machines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpMachineResource {

    @ApiOperation("获取所有的虚拟机母机信息")
    @GET
    @Path("/")
    fun list(
        @ApiParam(value = "IP", required = false)
        @QueryParam(value = "ip")
        ip: String?,
        @ApiParam(value = "主机名", required = false)
        @QueryParam(value = "name")
        name: String?,
        @ApiParam(value = "用户名", required = false)
        @QueryParam(value = "username")
        username: String?
    ): Result<List<Machine>>

    @ApiOperation("虚拟机母机入库")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "Machine 信息", required = true)
        machine: MachineCreate
    ): Result<Boolean>

    @ApiOperation("删除虚拟机母机")
    @DELETE
    @Path("/{id}")
    fun delete(
        @ApiParam(value = "Machine ID", required = true)
        @PathParam("id") id: Int
    ): Result<Boolean>

    @ApiOperation("更改虚拟机母机")
    @PUT
    @Path("/")
    fun update(
        @ApiParam(value = "Machine 信息", required = true)
        machine: MachineCreate
    ): Result<Boolean>
}