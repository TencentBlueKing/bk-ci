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
 *
 */

package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_RESOURCE_GROUP"], description = "用户态-iam用户组")
@Path("/user/auth/resource/group/{projectId}/{resourceType}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthResourceGroupResource {

    @GET
    @Path("{groupId}/groupPolicies")
    @ApiOperation("获取组策略详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].action}"])
    fun getGroupPolicies(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<List<IamGroupPoliciesVo>>

    @PUT
    @Path("{groupId}/member/renewal")
    @ApiOperation("用户续期")
    fun renewal(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("用户组Id")
        @PathParam("groupId")
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Result<Boolean>

    @DELETE
    @Path("{groupId}/member")
    @ApiOperation("用户退出")
    fun deleteMember(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<Boolean>

    @DELETE
    @Path("{groupId}")
    @ApiOperation("删除组")
    fun deleteGroup(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("用户组Id")
        @PathParam("groupId")
        groupId: Int
    ): Result<Boolean>

    @PUT
    @Path("{groupId}/rename")
    @ApiOperation("重命名组")
    fun rename(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("用户组Id")
        @PathParam("groupId")
        groupId: Int,
        renameGroupDTO: RenameGroupDTO
    ): Result<Boolean>
}
