package com.tencent.devops.process.trigger.api.external

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

/**
 * 与 process 的 [com.tencent.devops.process.api.external.ExternalScmResource] 路径一致，仅在 trigger 独立部署时加载
 */
@Tag(name = "EXTERNAL_TRIGGER_SCM", description = "外部-Trigger-代码库webhook")
@Path("/external/scm")
@ServiceInterface("trigger")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalTriggerScmResource {

    @Operation(summary = "Code平台SVN仓库提交")
    @POST
    @Path("/codesvn/commit")
    fun webHookCodeSvnCommit(event: String): Result<Boolean>

    @Operation(summary = "Code平台Git仓库提交")
    @POST
    @Path("/codegit/commit")
    fun webHookCodeGitCommit(
        @Parameter(description = "X-Event")
        @HeaderParam("X-Event")
        event: String,
        @Parameter(description = "X-Token")
        @HeaderParam("X-Token")
        secret: String? = null,
        @Parameter(description = "来源类型,是否是测试请求")
        @HeaderParam("X-Source-Type")
        sourceType: String? = null,
        @Parameter(description = "X-TRACE-ID")
        @HeaderParam("X-TRACE-ID")
        traceId: String,
        body: String
    ): Result<Boolean>

    @Operation(summary = "Gitlab仓库提交")
    @POST
    @Path("/gitlab/commit")
    fun webHookGitlabCommit(event: String): Result<Boolean>

    @Operation(summary = "Code平台tGit仓库提交")
    @POST
    @Path("/codetgit/commit")
    fun webHookCodeTGitCommit(
        @Parameter(description = "X-Event")
        @HeaderParam("X-Event")
        event: String,
        @Parameter(description = "X-Token")
        @HeaderParam("X-Token")
        secret: String? = null,
        @Parameter(description = "X-TRACE-ID")
        @HeaderParam("X-TRACE-ID")
        traceId: String,
        body: String
    ): Result<Boolean>

    @Operation(summary = "p4仓库提交")
    @POST
    @Path("/p4/commit")
    fun webHookCodeP4Commit(body: String): Result<Boolean>

    @Operation(summary = "通用仓库提交")
    @POST
    @Path("/{scmCode}/commit")
    fun webHookScmCommit(
        @PathParam("scmCode")
        @Parameter(description = "仓库scmCode")
        scmCode: String,
        @Context request: HttpServletRequest,
        body: String
    ): Result<Boolean>
}
