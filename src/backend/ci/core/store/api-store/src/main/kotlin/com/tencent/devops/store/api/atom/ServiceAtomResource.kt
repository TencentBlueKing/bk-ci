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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomClassifyInfo
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.PipelineAtom
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PIPELINE_ATOM", description = "流水线-插件")
@Path("/service/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAtomResource {

    @Operation(summary = "获取项目下已安装的插件列表")
    @GET
    @Path("/projectCodes/{projectCode}/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data[*].atomCode}", "{data[*].version}", "releaseInfo"]
    )
    fun getInstalledAtoms(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<InstalledAtom>>

    @Operation(summary = "根据插件代码和版本号获取插件详细信息")
    @GET
    @Path("/codes/{atomCode}/versions/{version}")
    @BkInterfaceI18n(
        keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"]
    )
    fun getAtomVersionInfo(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<PipelineAtom?>

    @Operation(summary = "根据插件代码和版本号集合批量获取插件信息")
    @POST
    @Path("/list/atomInfos")
    fun getAtomInfos(
        @Parameter(description = "插件代码版本集合", required = true)
        codeVersions: Set<AtomCodeVersionReqItem>
    ): Result<List<AtomRunInfo>>

    @Operation(summary = "获取插件真实版本号")
    @GET
    @Path("/projects/{projectCode}/codes/{atomCode}/versions/{version}/real")
    fun getAtomRealVersion(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<String?>

    @Operation(summary = "获取插件属性列表")
    @POST
    @Path("/prop/list")
    fun getAtomProps(
        @Parameter(description = "插件标识列表", required = true)
        atomCodes: Set<String>
    ): Result<Map<String, AtomProp>?>

    @Operation(summary = "获取插件分类信息")
    @GET
    @Path("/codes/{atomCode}/classify/info")
    @BkInterfaceI18n(keyPrefixNames = ["ATOM", "{data.atomCode}", "{data.version}", "releaseInfo"])
    fun getAtomClassifyInfo(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<AtomClassifyInfo?>
}
