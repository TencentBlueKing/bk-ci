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
 *
 */

package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_RESOURCE_GROUP", description = "用户态-iam用户组")
@Path("/user/auth/resource/group/{projectId}/{resourceType}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface UserAuthResourceGroupResource {

    @GET
    @Path("{groupId}/groupPolicies")
    @Operation(summary = "获取组策略详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].action}"])
    fun getGroupPolicies(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<List<IamGroupPoliciesVo>>

    @GET
    @Path("getMemberGroupsDetails")
    @Operation(summary = "获取项目成员有权限的用户组详情")
    fun getMemberGroupsDetails(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @QueryParam("memberId")
        @Parameter(description = "组织ID/成员ID")
        memberId: String,
        @QueryParam("groupName")
        @Parameter(description = "用户组名称")
        groupName: String?,
        @QueryParam("minExpiredAt")
        @Parameter(description = "最小过期时间")
        minExpiredAt: Long?,
        @QueryParam("maxExpiredAt")
        @Parameter(description = "最大过期时间")
        maxExpiredAt: Long?,
        @QueryParam("relatedResourceType")
        @Parameter(description = "资源类型")
        relatedResourceType: String?,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "资源ID")
        relatedResourceCode: String?,
        @QueryParam("action")
        @Parameter(description = "操作")
        action: String?,
        @QueryParam("operateChannel")
        @Parameter(description = "操作渠道")
        operateChannel: OperateChannel?,
        @QueryParam("uniqueManagerGroupsQueryFlag")
        @Parameter(description = "唯一管理员组查询标识")
        uniqueManagerGroupsQueryFlag: Boolean?,
        @Parameter(description = "起始位置,从0开始")
        @QueryParam("start")
        start: Int,
        @Parameter(description = "每页多少条")
        @QueryParam("limit")
        limit: Int
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @GET
    @Path("{groupId}/getMemberGroupDetails/")
    @Operation(summary = "获取用户加入单个组的详情")
    fun getMemberGroupDetails(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int,
        @QueryParam("memberId")
        @Parameter(description = "组织ID/成员ID")
        memberId: String
    ): Result<GroupDetailsInfoVo>

    @PUT
    @Path("{groupId}/member/renewal")
    @Operation(summary = "用户续期")
    fun renewal(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Result<Boolean>

    @DELETE
    @Path("{groupId}/member")
    @Operation(summary = "用户退出")
    fun deleteMember(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<Boolean>

    @DELETE
    @Path("{groupId}")
    @Operation(summary = "删除组")
    fun deleteGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<Boolean>

    @PUT
    @Path("{groupId}/rename")
    @Operation(summary = "重命名组")
    fun rename(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组Id")
        @PathParam("groupId")
        groupId: Int,
        renameGroupDTO: RenameGroupDTO
    ): Result<Boolean>
}
