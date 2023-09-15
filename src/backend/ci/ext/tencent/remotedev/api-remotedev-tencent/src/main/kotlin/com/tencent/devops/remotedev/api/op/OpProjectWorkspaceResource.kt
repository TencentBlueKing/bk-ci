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

package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PROJECT_WORKSPACE"], description = "OP_PROJECT_WORKSPACE")
@Path("/op/project/workspace")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectWorkspaceResource {

    @ApiOperation("分配云桌面")
    @POST
    @Path("/assign")
    fun assignWorkspace(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "拥有者", required = true)
        @QueryParam("owner")
        owner: String? = null,
        @ApiParam(value = "云桌面ID", required = true)
        @QueryParam("cgsIds")
        cgsIds: List<String>
    ): Result<Boolean>

    @ApiOperation("获取项目下空间列表实例列表")
    @GET
    @Path("/list")
    fun getProjectWorkspaceList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "projectId", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam(value = "workspaceName", required = false)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @ApiParam(value = "systemType", required = false)
        @QueryParam("systemType")
        systemType: WorkspaceSystemType?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>>
}
