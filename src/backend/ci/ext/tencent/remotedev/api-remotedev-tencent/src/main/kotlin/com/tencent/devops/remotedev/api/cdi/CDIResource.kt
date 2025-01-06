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

package com.tencent.devops.remotedev.api.cdi

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.common.DEVX_HEADER_CDI_WORKSPACE_NAME
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/*
* CDI接口规范：
* 1.接口入参需有 AUTH_HEADER_USER_ID ，并且限制为云桌面当前登陆人，如果当前无登陆人则值为no_login_user。
* 2.接口入参需有 AUTH_HEADER_DEVOPS_STORE_CODE ，并且限制为当前访问应用ID
* 3.接口入参需有 DEVX_HEADER_CDI_WORKSPACE_NAME ，并且限制为当前云桌面
* */
@Tag(name = "CDI_REMOTE_DEV", description = "cdi-remoteDev")
@Path("/cdi/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface CDIResource {
    @Operation(summary = "云桌面应用获取云桌面信息")
    @GET
    @Path("/workspace_detail")
    fun getWorkspaceDetail(
        @Parameter(description = "userId", required = false)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "应用id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_CODE)
        storeCode: String,
        @Parameter(description = "workspaceName", required = false)
        @HeaderParam(DEVX_HEADER_CDI_WORKSPACE_NAME)
        workspaceName: String
    ): Result<WeSecProjectWorkspace?>

    @Operation(summary = "云桌面应用获取当前实例登陆人")
    @GET
    @Path("/login_user_id")
    fun getLoginUserId(
        @Parameter(description = "userId", required = false)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "应用id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_CODE)
        storeCode: String,
        @Parameter(description = "workspaceName", required = false)
        @HeaderParam(DEVX_HEADER_CDI_WORKSPACE_NAME)
        workspaceName: String
    ): Result<String/*当前登陆人id*/>

    @Operation(summary = "云桌面应用给云桌面或者客户端发送消息")
    @POST
    @Path("/notify")
    fun messageRegister(
        @Parameter(description = "userId", required = false)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "应用id", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_STORE_CODE)
        storeCode: String,
        @Parameter(description = "workspaceName", required = false)
        @HeaderParam(DEVX_HEADER_CDI_WORKSPACE_NAME)
        workspaceName: String,
        data: WorkspaceDesktopNotifyData
    ): Result<Boolean>
}
