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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
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

@Tag(name = "OP_PIPELINE_ATOM", description = "OP-流水线-插件")
@Path("/op/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAtomResource {

    @Operation(summary = "添加流水线插件")
    @POST
    @Path("/")
    fun add(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线插件请求报文体", required = true)
        atomCreateRequest: AtomCreateRequest
    ): Result<Boolean>

    @Operation(summary = "更新流水线插件信息")
    @PUT
    @Path("/{id}")
    fun update(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线插件ID", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "流水线插件请求报文体", required = true)
        atomUpdateRequest: AtomUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "获取所有流水线插件信息")
    @GET
    @Path("/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun listAllPipelineAtoms(
        @Parameter(description = "插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @Parameter(description = "插件标识", required = false)
        @QueryParam("atomCode")
        atomCode: String?,
        @Parameter(description = "插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
        @QueryParam("atomType")
        atomType: AtomTypeEnum?,
        @Parameter(description = "支持的服务范围（pipeline/quality/all 分别表示流水线/质量红线/全部）", required = false)
        @QueryParam("serviceScope")
        serviceScope: String?,
        @Parameter(description = "操作系统（ALL/WINDOWS/LINUX/MACOS）", required = false)
        @QueryParam("os")
        os: String?,
        @Parameter(description = "插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = false)
        @QueryParam("category")
        category: String?,
        @Parameter(description = "插件分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "插件状态", required = false)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: OpSortTypeEnum? = OpSortTypeEnum.UPDATE_TIME,
        @Parameter(description = "排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<AtomResp<Atom>?>

    @Operation(summary = "根据ID获取流水线插件信息")
    @GET
    @Path("/{id}")
    @BkInterfaceI18n(keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"])
    fun getPipelineAtomById(
        @Parameter(description = "流水线插件ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Atom?>

    @Operation(summary = "根据ID获取流水线插件信息")
    @DELETE
    @Path("/{id}")
    fun deletePipelineAtomById(
        @Parameter(description = "流水线插件ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>

    @Operation(summary = "审核插件")
    @Path("/{atomId}/approve")
    @PUT
    fun approveAtom(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "审核插件请求报文")
        approveReq: ApproveReq
    ): Result<Boolean>

    @Operation(summary = "生成ci yaml")
    @GET
    @Path("/gitci/generate")
    fun generateCiYaml(
        @Parameter(description = "原子插件标识", required = false)
        @QueryParam("atomCode")
        atomCode: String?,
        @Parameter(description = "操作系统", required = false)
        @QueryParam("os")
        os: String?,
        @Parameter(description = "插件类型 marketBuild:有编译环境,marketBuildLess:无编译环境", required = false)
        @QueryParam("classType")
        classType: String?,
        @Parameter(description = "是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String>

    @Operation(summary = "下架插件")
    @PUT
    @Path("/offline/atomCodes/{atomCode}/versions")
    fun offlineAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "下架插件请求报文")
        atomOfflineReq: AtomOfflineReq
    ): Result<Boolean>

    @Operation(summary = "根据插件包一键部署插件")
    @POST
    @Path("/deploy")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun releaseAtom(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @FormDataParam("atomCode")
        atomCode: String,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition,
        @Parameter(description = "发布者", required = false)
        @QueryParam("publisher")
        publisher: String? = null,
        @Parameter(description = "发布类型", required = false)
        @QueryParam("releaseType")
        releaseType: ReleaseTypeEnum? = null,
        @Parameter(description = "插件版本", required = false)
        @QueryParam("version")
        version: String? = null
    ): Result<Boolean>

    @Operation(summary = "设置插件为默认插件")
    @POST
    @Path("/default/atomCodes/{atomCode}")
    fun setDefault(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>

    @Operation(summary = "补充插件仓库标识")
    @POST
    @Path("/updateAtomRepoFlag")
    fun updateAtomRepoFlag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = false)
        @QueryParam("atomCode")
        atomCode: String?
    ): Result<Boolean>

    @Operation(summary = "刷新插件配置缓存")
    @POST
    @Path("/updateAtomConfigCache")
    fun updateAtomConfigCache(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "kProperty", required = false)
        @QueryParam("kProperty")
        kProperty: String,
        @Parameter(description = "atomCode", required = false)
        @QueryParam("atomCode")
        atomCode: String?
    ): Result<Boolean>
}
