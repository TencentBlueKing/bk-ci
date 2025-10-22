/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.web.annotation.BkField
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SHARDING_ROUTING_RULE", description = "SERVICE-DB分片规则")
@Path("/service/sharding/routing/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceShardingRoutingRuleResource {

    @Operation(summary = "根据名称获取分片规则信息")
    @GET
    @Path("/names/{routingName}/get")
    fun getShardingRoutingRuleByName(
        @Parameter(description = "规则名称", required = true)
        @PathParam("routingName")
        @BkField(minLength = 1, maxLength = 128)
        routingName: String,
        @Parameter(description = "模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: SystemModuleEnum,
        @Parameter(description = "规则类型", required = true)
        @QueryParam("ruleType")
        ruleType: ShardingRuleTypeEnum,
        @Parameter(description = "数据库表名称", required = false)
        @QueryParam("tableName")
        @BkField(minLength = 1, maxLength = 128, required = false)
        tableName: String? = null
    ): Result<ShardingRoutingRule?>

    @PUT
    @Path("/update")
    @Operation(summary = "更新项目DB分片路由规则")
    fun updateShardingRoutingRule(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "路由规则", required = true)
        shardingRoutingRule: ShardingRoutingRule
    ): Result<Boolean>
}
