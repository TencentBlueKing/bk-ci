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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.version.VersionInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_ATOM", description = "流水线-插件")
@Path("/user/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserAtomResource {

    @Operation(summary = "获取所有流水线插件信息")
    @GET
    @Path("/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun listAllPipelineAtoms(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "支持的服务范围（pipeline/quality/all 分别表示流水线/质量红线/全部）", required = false)
        @QueryParam("serviceScope")
        serviceScope: String?,
        @Parameter(description = "job类型，AGENT： 编译环境，AGENT_LESS：无编译环境", required = false)
        @QueryParam("jobType")
        jobType: String?,
        @Parameter(description = "操作系统（ALL/WINDOWS/LINUX/MACOS）", required = false)
        @QueryParam("os")
        os: String?,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = false)
        @QueryParam("category")
        category: String? = AtomCategoryEnum.TASK.name,
        @Parameter(description = "插件分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "是否推荐", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "查询项目插件标识", required = true)
        @QueryParam("queryProjectAtomFlag")
        queryProjectAtomFlag: Boolean = true,
        @Parameter(description = "是否适配操作系统标识", required = false)
        @QueryParam("fitOsFlag")
        fitOsFlag: Boolean? = true,
        @Parameter(description = "查询支持有编译环境下的无编译环境插件标识", required = false)
        @QueryParam("queryFitAgentBuildLessAtomFlag")
        queryFitAgentBuildLessAtomFlag: Boolean? = true,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<AtomResp<AtomRespItem>?>

    @Operation(summary = "根据插件代码和版本号获取流水线插件详细信息")
    @GET
    @Path("/{projectCode}/{atomCode}/{version}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getPipelineAtom(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String,
        @Parameter(description = "是否查询已下架版本", required = false)
        @QueryParam("queryOfflineFlag")
        queryOfflineFlag: Boolean? = true
    ): Result<PipelineAtom?>

    @Operation(summary = "根据插件插件代码获取对应的版本列表信息")
    @GET
    @Path("/projectCodes/{projectCode}/atomCodes/{atomCode}/version/list")
    fun getPipelineAtomVersions(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<List<VersionInfo>>

    @Operation(summary = "获取项目下已安装的插件列表")
    @GET
    @Path("/projectCodes/{projectCode}/installedAtoms/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun getInstalledAtoms(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "名称", required = false)
        @QueryParam("name")
        name: String?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<Page<InstalledAtom>>

    @Operation(summary = "更新流水线插件信息")
    @PUT
    @Path("/baseInfo/atoms/{atomCode}")
    fun updateAtomBaseInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件代码 ", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件基本信息修改请求报文体", required = true)
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "卸载插件")
    @Path("/projectCodes/{projectCode}/atoms/{atomCode}")
    @DELETE
    fun uninstallAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码 ", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "卸载插件请求包体", required = true)
        unInstallReq: UnInstallReq
    ): Result<Boolean>

    @Operation(summary = "批量获取插件输出信息")
    @POST
    @Path("/output/info/list")
    fun getAtomOutputInfos(
        @Parameter(description = "插件信息集合，格式：插件标识@版本号", required = true)
        atomInfos: Set<String>
    ): Result<Map<String, String>?>
}
