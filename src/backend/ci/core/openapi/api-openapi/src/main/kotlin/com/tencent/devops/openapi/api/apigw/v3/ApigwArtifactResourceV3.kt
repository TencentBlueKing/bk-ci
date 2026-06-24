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
package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 产出物元数据查询 OpenAPI
 * 供第三方系统（如 BKM）查询元数据
 */
@Tag(name = "OPENAPI_ARTIFACT_V3", description = "OPENAPI-产出物元数据")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/artifacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v3")
interface ApigwArtifactResourceV3 {

    /**
     * 查询产出物元数据
     *
     * @param appCode 应用Code
     * @param apigwType apigw类型
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param pipelineId 流水线ID（可选）
     * @param artifactType 产出物类型：FILE/IMAGE/REPORT/PACKAGE等
     * @param artifactName 产出物名称，如文件名、镜像名
     * @param artifactVersion 产出物版本，如镜像Tag、包版本
     * @return 产出物元数据
     */
    @Operation(
        summary = "查询产出物元数据",
        description = "根据项目ID、产出物类型等条件查询元数据，包含代码库地址和Commit ID"
    )
    @GET
    @Path("/{artifactType}")
    fun getArtifactInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID（可选）", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等", required = true)
        @PathParam("artifactType")
        artifactType: String,
        @Parameter(description = "产出物名称，如文件名、镜像名（可选）", required = false)
        @QueryParam("artifactName")
        artifactName: String?,
        @Parameter(description = "产出物版本，如镜像Tag、包版本（可选）", required = false)
        @QueryParam("artifactVersion")
        artifactVersion: String?
    ): Result<PipelineArtifactInfo?>
}
