package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_WEBHOOK", description = "用户-webhook")
@Path("/op/webhook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpScmWebhookResource {

    @Operation(summary = "更新所有的webhook项目名")
    @PUT
    @Path("/updateProjectNameAndTaskId")
    fun updateProjectNameAndTaskId(): Result<Boolean>

    @Operation(summary = "更新webhook secret")
    @PUT
    @Path("/updateWebhookSecret")
    fun updateWebhookSecret(
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("scmType")
        scmType: String
    ): Result<Boolean>

    @Operation(summary = "更新webhook 事件信息")
    @PUT
    @Path("/updateWebhookEventInfo")
    fun updateWebhookEventInfo(
        @Parameter(description = "待更新的项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Boolean>

    @Operation(summary = "更新webhook projectName")
    @PUT
    @Path("/updateWebhookProjectName")
    fun updateWebhookProjectName(
        @Parameter(description = "待更新的项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "待更新的流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>

    @Operation(summary = "添加scm灰度白名单仓库")
    @PUT
    @Path("/{scmCode}/addGrayRepoWhite")
    fun addGrayRepoWhite(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "是否只开启pac仓库", required = true)
        @QueryParam("pac")
        pac: Boolean,
        @Parameter(description = "服务端代码仓库id列表", required = true)
        serverRepoNames: List<String>
    ): Result<Boolean>

    @Operation(summary = "移除scm灰度白名单仓库")
    @PUT
    @Path("/{scmCode}/removeGrayRepoWhite")
    fun removeGrayRepoWhite(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "是否只开启pac仓库", required = true)
        @QueryParam("pac")
        pac: Boolean,
        @Parameter(description = "服务端代码仓库id列表", required = true)
        serverRepoNames: List<String>
    ): Result<Boolean>

    @Operation(summary = "移除scm灰度白名单仓库")
    @GET
    @Path("/{scmCode}/isGrayRepoWhite")
    fun isGrayRepoWhite(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "是否只开启pac仓库", required = true)
        @QueryParam("pac")
        pac: Boolean,
        @Parameter(description = "服务端代码仓库id", required = true)
        @QueryParam("serverRepoName")
        serverRepoName: String
    ): Result<Boolean>

    @Operation(summary = "添加scm灰度黑名单仓库")
    @PUT
    @Path("/{scmCode}/addGrayRepoBlack")
    fun addGrayRepoBlack(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "服务端代码仓库id列表", required = true)
        serverRepoNames: List<String>
    ): Result<Boolean>

    @Operation(summary = "移除scm灰度黑名单仓库")
    @PUT
    @Path("/{scmCode}/removeGrayRepoBlack")
    fun removeGrayRepoBlack(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "服务端代码仓库id列表", required = true)
        serverRepoNames: List<String>
    ): Result<Boolean>

    @Operation(summary = "移除scm灰度黑名单仓库")
    @GET
    @Path("/{scmCode}/isGrayRepoBlack")
    fun isGrayRepoBlack(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "服务端代码仓库id", required = true)
        @QueryParam("serverRepoName")
        serverRepoName: String
    ): Result<Boolean>

    @Operation(summary = "更新scm灰度权重")
    @PUT
    @Path("/{scmCode}/updateGrayRepoWeight")
    fun updateGrayRepoWeight(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "灰度权重", required = true)
        @QueryParam("weight")
        weight: String
    ): Result<Boolean>

    @Operation(summary = "获取scm灰度权重")
    @GET
    @Path("/{scmCode}/getGrayRepoWeight")
    fun getGrayRepoWeight(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<String>
}
