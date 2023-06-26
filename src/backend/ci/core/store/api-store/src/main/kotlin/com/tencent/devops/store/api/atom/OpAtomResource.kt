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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE_ATOM"], description = "OP-流水线-插件")
@Path("/op/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAtomResource {

    @ApiOperation("添加流水线插件")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线插件请求报文体", required = true)
        atomCreateRequest: AtomCreateRequest
    ): Result<Boolean>

    @ApiOperation("更新流水线插件信息")
    @PUT
    @Path("/{id}")
    fun update(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("流水线插件ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "流水线插件请求报文体", required = true)
        atomUpdateRequest: AtomUpdateRequest
    ): Result<Boolean>

    @ApiOperation("获取所有流水线插件信息")
    @GET
    @Path("/")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.records[*].atomCode}", "{data.records[*].version}", "releaseInfo"]
    )
    fun listAllPipelineAtoms(
        @ApiParam("插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("插件标识", required = false)
        @QueryParam("atomCode")
        atomCode: String?,
        @ApiParam("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
        @QueryParam("atomType")
        atomType: AtomTypeEnum?,
        @ApiParam("支持的服务范围（pipeline/quality/all 分别表示流水线/质量红线/全部）", required = false)
        @QueryParam("serviceScope")
        serviceScope: String?,
        @ApiParam("操作系统（ALL/WINDOWS/LINUX/MACOS）", required = false)
        @QueryParam("os")
        os: String?,
        @ApiParam("插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = false)
        @QueryParam("category")
        category: String?,
        @ApiParam("插件分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @ApiParam("插件状态", required = false)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: OpSortTypeEnum? = OpSortTypeEnum.UPDATE_TIME,
        @ApiParam("排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int = 10
    ): Result<AtomResp<Atom>?>

    @ApiOperation("根据ID获取流水线插件信息")
    @GET
    @Path("/{id}")
    @BkInterfaceI18n(keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"])
    fun getPipelineAtomById(
        @ApiParam("流水线插件ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Atom?>

    @ApiOperation("根据ID获取流水线插件信息")
    @DELETE
    @Path("/{id}")
    fun deletePipelineAtomById(
        @ApiParam("流水线插件ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>

    @ApiOperation("审核插件")
    @Path("/{atomId}/approve")
    @PUT
    fun approveAtom(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @ApiParam("审核插件请求报文")
        approveReq: ApproveReq
    ): Result<Boolean>

    @ApiOperation("生成ci yaml")
    @GET
    @Path("/gitci/generate")
    fun generateCiYaml(
        @ApiParam("原子插件标识", required = false)
        @QueryParam("atomCode")
        atomCode: String?,
        @ApiParam("操作系统", required = false)
        @QueryParam("os")
        os: String?,
        @ApiParam("插件类型 marketBuild:有编译环境,marketBuildLess:无编译环境", required = false)
        @QueryParam("classType")
        classType: String?,
        @ApiParam("是否展示系统自带的yml信息", required = false)
        @QueryParam("defaultShowFlag")
        defaultShowFlag: Boolean?
    ): Result<String>

    @ApiOperation("下架插件")
    @PUT
    @Path("/offline/atomCodes/{atomCode}/versions")
    fun offlineAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("下架插件请求报文")
        atomOfflineReq: AtomOfflineReq
    ): Result<Boolean>

    @ApiOperation("根据插件包一键部署插件")
    @POST
    @Path("/deploy")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun releaseAtom(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @FormDataParam("atomCode")
        atomCode: String,
        @ApiParam("文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<Boolean>

    @ApiOperation("设置插件为默认插件")
    @POST
    @Path("/default/atomCodes/{atomCode}")
    fun setDefault(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>
}
