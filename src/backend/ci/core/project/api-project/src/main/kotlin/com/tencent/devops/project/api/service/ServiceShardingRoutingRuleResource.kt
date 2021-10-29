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

<<<<<<< HEAD:src/backend/ci/ext/tencent/plugin/api-plugin-tencent/src/main/kotlin/com/tencent/devops/plugin/api/BuildOnsResource.kt
package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.ons.OnsNameInfo
=======
package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.project.pojo.ShardingRoutingRule
>>>>>>> carl/issue_5267_sub_db:src/backend/ci/core/project/api-project/src/main/kotlin/com/tencent/devops/project/api/service/ServiceShardingRoutingRuleResource.kt
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

<<<<<<< HEAD:src/backend/ci/ext/tencent/plugin/api-plugin-tencent/src/main/kotlin/com/tencent/devops/plugin/api/BuildOnsResource.kt
/**
 * ons名字服务
 */
@Api(tags = ["BUILD_ONS"], description = "名字服务")
@Path("/build/ons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildOnsResource {

    @ApiOperation("获取无状态名字信息")
    @GET
    @Path("/host/domains/{domainName}")
    fun getHostByDomainName(
        @ApiParam("域名", required = true)
        @PathParam("domainName")
        domainName: String
    ): Result<OnsNameInfo?>
=======
@Api(tags = ["SERVICE_SHARDING_ROUTING_RULE"], description = "SERVICE-DB分片规则")
@Path("/service/sharding/routing/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceShardingRoutingRuleResource {

    @ApiOperation("根据名称获取分片规则信息")
    @GET
    @Path("/names/{routingName}/get")
    fun getShardingRoutingRuleByName(
        @ApiParam("规则名称", required = true)
        @PathParam("routingName")
        @BkField(minLength = 1, maxLength = 128)
        routingName: String,
    ): Result<ShardingRoutingRule?>
>>>>>>> carl/issue_5267_sub_db:src/backend/ci/core/project/api-project/src/main/kotlin/com/tencent/devops/project/api/service/ServiceShardingRoutingRuleResource.kt
}
