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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.`var`.`do`.PipelineRefPublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PUBLIC_VAR_GROUP_REFERENCE", description = "用户-公共变量组引用管理")
@Path("/user/public/var/groups/references")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPublicVarGroupReferenceResource {

    @Operation(summary = "获取引用变量的列表（模板或流水线）")
    @GET
    @Path("/{groupName}/list")
    fun listVarReferInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "变量名称", required = false)
        @QueryParam("varName")
        varName: String? = null,
        @Parameter(description = "引用类型", required = false)
        @QueryParam("referType")
        referType: PublicVerGroupReferenceTypeEnum? = null,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: Int? = null,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int
    ): Result<Page<PublicGroupVarRefDO>>

    @Operation(summary = "预览变更")
    @POST
    @Path("/changePreview")
    fun getChangePreview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "公共变量组请求报文", required = true)
        publicVarGroup: PublicVarGroupVO
    ): Result<List<PublicVarReleaseDO>>

    @Operation(summary = "获取关联的公共变量组信息")
    @GET
    @Path("/refers/{referId}/group/info")
    fun listPipelineVarGroupInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "引用资源ID", required = true)
        @PathParam("referId")
        referId: String,
        @Parameter(description = "引用资源类型", required = true)
        @QueryParam("referType")
        referType: PublicVerGroupReferenceTypeEnum,
        @Parameter(description = "引用版本号", required = true)
        @QueryParam("referVersion")
        referVersion: Int
    ): Result<List<PipelineRefPublicVarGroupDO>>

    @Operation(summary = "获取项目关联公共变量组信息")
    @GET
    @Path("/projects/group/info")
    fun listProjectVarGroupInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String
    ): Result<List<PipelineRefPublicVarGroupDO>>
}
