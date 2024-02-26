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

package com.tencent.devops.environment.api.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdPartyAgent.JDKInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Suppress("TooManyFunctions")
@Tag(name = "OP_ENVIRONMENT_UPGRADE_THIRD_PARTY_AGENT", description = "第三方构建机升级设置")
@Path("/op/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpThirdPartyAgentUpgradeResource {

    @Operation(summary = "设置JDK版本列表")
    @PUT
    @Path("/agents/set_jdk_version_list")
    fun setJDKVersionList(
        @Parameter(description = "osArchJDKVersionList", required = true)
        osArchJDKVersionSet: Set<JDKInfo>
    ): Result<Boolean>

    @Operation(summary = "设置agent最大并发升级数量")
    @PUT
    @Path("/agents/setMaxParallelUpgradeCount")
    fun setMaxParallelUpgradeCount(
        @Parameter(description = "maxParallelUpgradeCount", required = true)
        maxParallelUpgradeCount: Int
    ): Result<Boolean>

    @Operation(summary = "获取agent最大并发升级数量")
    @GET
    @Path("/agents/getMaxParallelUpgradeCount")
    fun getMaxParallelUpgradeCount(): Result<Int?>

    @Operation(summary = "设置Agent Worker升级版本")
    @PUT
    @Path("/agents/upgrade/{version}")
    fun setAgentWorkerVersion(
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<Boolean>

    @Operation(summary = "设置Agent Master版本")
    @PUT
    @Path("/agents/masterVersion/{version}")
    fun setMasterVersion(
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<Boolean>

    @Operation(summary = "获取当前Agent Worker版本")
    @GET
    @Path("/agent/upgrade")
    fun getAgentWorkerVersion(): Result<String?>

    @Operation(summary = "获取当前Master版本")
    @GET
    @Path("/agent/masterVersion")
    fun getAgentMasterVersion(): Result<String?>

    @Operation(summary = "设置agent强制升级")
    @PUT
    @Path("/agents/setForceUpdateAgents")
    fun setForceUpdateAgents(
        @Parameter(description = "agent long id", required = true)
        agentIds: List<Long>,
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "取消agent强制升级")
    @DELETE
    @Path("/agents/unsetForceUpdateAgents")
    fun unsetForceUpdateAgents(
        @Parameter(description = "agent long id", required = true)
        agentIds: List<Long>,
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "获取所有强制升级agent(agent long id)")
    @GET
    @Path("/agents/getAllForceUpgradeAgents")
    fun getAllForceUpgradeAgents(
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Set<Long>> /* agent long id */

    @Operation(summary = "取消所有强制升级agent")
    @DELETE
    @Path("/agents/cleanAllForceUpgradeAgents")
    fun cleanAllForceUpgradeAgents(
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "设置agent锁定升级")
    @PUT
    @Path("/agents/setLockUpdateAgents")
    fun setLockUpdateAgents(
        @Parameter(description = "agent long id", required = true)
        agentIds: List<Long>,
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "取消agent锁定升级")
    @DELETE
    @Path("/agents/unsetLockUpdateAgents")
    fun unsetLockUpdateAgents(
        @Parameter(description = "agent long id", required = true)
        agentIds: List<Long>,
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "获取所有强制锁定agent")
    @GET
    @Path("/agents/getAllLockUpgradeAgents")
    fun getAllLockUpgradeAgents(
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Set<Long>> /* agent long id */

    @Operation(summary = "取消所有强制锁定agent")
    @DELETE
    @Path("/agents/cleanAllLockUpgradeAgents")
    fun cleanAllLockUpgradeAgents(
        @QueryParam("upgradeType")
        agentUpgradeType: String?
    ): Result<Boolean>

    @Operation(summary = "设置优先升级项目（指定项目）")
    @PUT
    @Path("/agents/set_priority_upgrade_projects")
    fun setPriorityUpgradeAgentProjects(
        @Parameter(description = "projectIds", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "取消优先升级项目（指定项目）")
    @DELETE
    @Path("/agents/unset_priority_upgrade_projects")
    fun unsetPriorityUpgradeAgentProjects(
        @Parameter(description = "projectIds", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "获取优先升级项目列表(所有）")
    @GET
    @Path("/agents/get_all_priority_upgrade_projects")
    fun getAllPriorityUpgradeAgentProjects(): Result<Set<String>>

    @Operation(summary = "取消优先升级项目（所有）")
    @DELETE
    @Path("/agents/clean_all_priority_upgrade_projects")
    fun cleanAllPriorityUpgradeAgentProjects(): Result<Boolean>

    @Operation(summary = "设置禁止升级项目（指定项目）")
    @PUT
    @Path("/agents/set_deny_upgrade_projects")
    fun setDenyUpgradeAgentProjects(
        @Parameter(description = "projectIds", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "移除禁止升级项目（指定项目）")
    @DELETE
    @Path("/agents/unset_deny_upgrade_projects")
    fun unsetDenyUpgradeAgentProjects(
        @Parameter(description = "agentIds", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "获取禁止升级项目列表(所有）")
    @GET
    @Path("/agents/get_all_deny_upgrade_projects")
    fun getAllDenyUpgradeAgentProjects(): Result<Set<String>>

    @Operation(summary = "移除禁止升级项目（所有）")
    @DELETE
    @Path("/agents/clean_all_deny_upgrade_projects")
    fun cleanAllDenyUpgradeAgentProjects(): Result<Boolean>
}
