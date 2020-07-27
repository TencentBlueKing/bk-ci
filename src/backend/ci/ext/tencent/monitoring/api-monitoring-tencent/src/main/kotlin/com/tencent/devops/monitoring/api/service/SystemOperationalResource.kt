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
package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.monitoring.pojo.Incident
import com.tencent.devops.monitoring.pojo.SystemOperational
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
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MONITORING_INCIDENTS"], description = "故障管理")
@Path("/service/system")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface SystemOperationalResource {

    @ApiOperation("查询系统可用性")
    @GET
    @Path("/operational")
    fun operational(): Result<SystemOperational>

    @ApiOperation("添加故障")
    @PUT
    @Path("/incidents")
    fun addIncidents(
        @ApiParam("故障", required = true)
        incident: Incident
    ): Result<Long>

    @ApiOperation("修改故障")
    @POST
    @Path("/incidents")
    fun updateIncidents(
        @ApiParam("故障", required = true)
        incident: Incident
    ): Result<Boolean>

    @ApiOperation("删除故障")
    @DELETE
    @Path("/incidents/{incidentId}")
    fun deleteIncidents(
        @ApiParam("故障", required = true)
        @PathParam("incidentId")
        incidentId: Long
    ): Result<Boolean>

    @ApiOperation("查看故障")
    @GET
    @Path("/incidents/{incidentId}")
    fun getIncidents(
        @ApiParam("故障", required = true)
        @PathParam("incidentId")
        incidentId: Long
    ): Result<Incident?>
}