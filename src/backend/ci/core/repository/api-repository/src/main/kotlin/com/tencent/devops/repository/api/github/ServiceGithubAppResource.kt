package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.sdk.github.request.GetAppInstallationForOrgRequest
import com.tencent.devops.repository.sdk.github.request.GetAppInstallationForRepoRequest
import com.tencent.devops.repository.sdk.github.response.GetAppInstallationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_APP_GITHUB", description = "服务-github-app")
@Path("/service/github/app")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubAppResource {

    @Operation(summary = "获取仓库app安装")
    @POST
    @Path("/repo")
    fun getAppInstallationForRepo(request: GetAppInstallationForRepoRequest): Result<GetAppInstallationResponse?>

    @Operation(summary = "获取组织app安装")
    @POST
    @Path("/org")
    fun getAppInstallationForOrg(request: GetAppInstallationForOrgRequest): Result<GetAppInstallationResponse?>

    @Operation(summary = "判断仓库是否安装github app")
    @POST
    @Path("/isInstallApp")
    fun isInstallApp(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        @Parameter(description = "仓库Id")
        @QueryParam("repoName")
        repoName: String
    ): Result<AppInstallationResult>
}
