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
import com.tencent.devops.process.pojo.`var`.`do`.PipelinePublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupYamlStringVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "USER_PUBLIC_VAR_GROUP", description = "用户-公共变量组")
@Path("/user/pipeline/public/var/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPublicVarGroupResource {

    @Operation(summary = "添加公共变量组")
    @POST
    @Path("/projects/{projectId}/add")
    fun addGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作类型", required = true)
        @QueryParam("operateType")
        operateType: OperateTypeEnum,
        @Parameter(description = "公共变量组请求报文", required = true)
        publicVarGroup: PublicVarGroupVO
    ): Result<String>

    @Operation(summary = "查询公共变量组列表")
    @GET
    @Path("/projects/{projectId}/list")
    fun getGroups(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "按变量组名称过滤", required = false)
        @QueryParam("filterByGroupName")
        filterByGroupName: String?,
        @Parameter(description = "按变量组描述过滤", required = false)
        @QueryParam("filterByGroupDesc")
        filterByGroupDesc: String?,
        @Parameter(description = "按更新人过滤", required = false)
        @QueryParam("filterByUpdater")
        filterByUpdater: String?,
        @Parameter(description = "按变量名称过滤", required = false)
        @QueryParam("filterByVarName")
        filterByVarName: String? = null,
        @Parameter(description = "按变量别名过滤", required = false)
        @QueryParam("filterByVarAlias")
        filterByVarAlias: String? = null,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<PublicVarGroupDO>>

    @Operation(summary = "获取项目下公共变量组名称列表")
    @GET
    @Path("/projects/{projectId}/names")
    fun getGroupNames(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<String>>

    @Operation(summary = "导入公共变量组(YAML格式)")
    @POST
    @Path("/projects/{projectId}/import")
    fun importGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作类型", required = true)
        @QueryParam("operateType")
        operateType: OperateTypeEnum,
        @Parameter(description = "YAML文件", required = true)
        yaml: PublicVarGroupYamlStringVO
    ): Result<String>

    @Operation(summary = "导出公共变量组(YAML格式)")
    @GET
    @Path("/projects/{projectId}/groups/{groupName}/export")
    fun exportGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: Int? = null
    ): Response

    @Operation(summary = "删除变量组")
    @DELETE
    @Path("/{groupName}")
    fun deleteGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String
    ): Result<Boolean>

    @Operation(summary = "获取引用变量的列表（模板或流水线）")
    @GET
    @Path("/{groupName}/references")
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

    @Operation(summary = "转换为变量组的YAML内容")
    @POST
    @Path("/projects/{projectId}/convert")
    fun convertGroupYaml(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "公共变量组请求报文", required = true)
        publicVarGroup: PublicVarGroupVO
    ): Result<String>

    @Operation(summary = "将YAML内容转换为变量组")
    @POST
    @Path("/projects/{projectId}/yaml/convert")
    fun convertYamlToGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "YAML内容", required = true)
        yaml: PublicVarGroupYamlStringVO
    ): Result<PublicVarGroupVO>

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
    ): Result<List<PipelinePublicVarGroupDO>>

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
    ): Result<List<PipelinePublicVarGroupDO>>

    @Operation(summary = "获取公共变量组版本历史")
    @GET
    @Path("/{groupName}/releaseHistory")
    fun getReleaseHistory(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<PublicVarReleaseDO>>
}
