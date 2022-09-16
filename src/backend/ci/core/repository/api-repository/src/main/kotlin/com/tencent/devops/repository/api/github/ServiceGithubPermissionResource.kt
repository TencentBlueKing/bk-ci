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

package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PERMISSION_GITHUB"], description = "服务-github-权限")
@Path("/service/github/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubPermissionResource {

    @ApiOperation("判断github是否是公开项目")
    @GET
    @Path("isPublicProject")
    fun isPublicProject(
        @ApiParam("授权用户ID", required = true)
        @QueryParam("authUserId")
        authUserId: String,
        @ApiParam("github项目名", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<Boolean>

    @ApiOperation("判断是否是项目成员")
    @GET
    @Path("isProjectMember")
    fun isProjectMember(
        @ApiParam("授权用户ID", required = true)
        @QueryParam("authUserId")
        authUserId: String,
        @ApiParam("授权用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("github项目名", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<Boolean>

    @ApiOperation("判断用户是否有权限")
    @GET
    @Path("checkUserAuth")
    fun checkUserAuth(
        @ApiParam("授权用户ID", required = true)
        @QueryParam("authUserId")
        authUserId: String,
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam("accessLevel", required = true)
        @QueryParam("accessLevel")
        accessLevel: Int
    ): Result<Boolean>
}
