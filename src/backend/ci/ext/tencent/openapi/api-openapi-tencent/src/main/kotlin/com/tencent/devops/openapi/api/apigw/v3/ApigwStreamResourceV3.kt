package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.stream.pojo.GitCIBuildHistory
import com.tencent.devops.stream.pojo.GitCIModelDetail
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.enums.GitCIProjectType
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.pojo.v2.GitCIUpdateSetting
import com.tencent.devops.stream.pojo.v2.GitUserValidateRequest
import com.tencent.devops.stream.pojo.v2.GitUserValidateResult
import com.tencent.devops.stream.pojo.v2.project.ProjectCIInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_STREAM"], description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/stream/gitProjects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwStreamResourceV3 {

    @ApiOperation("人工TriggerBuild启动构建")
    @POST
    @Path("/{gitProjectId}/pipelines/{pipelineId}/startup")
    fun triggerStartup(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("TriggerBuild请求", required = true)
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<TriggerBuildResult>

    @ApiOperation("工蜂project转换为streamProject")
    @GET
    @Path("/{gitProjectId}/projectName/transfer")
    fun getStreamProject(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String
    ): Result<String>

    @ApiOperation("项目下所有Stream流水线概览")
    @GET
    @Path("/{gitProjectId}/pipelines/list")
    fun getPipelineList(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条（最大50）", required = false, defaultValue = "10", allowableValues = "range[1, 50]")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<GitProjectPipeline>>

    @ApiOperation("获取指定Stream流水线信息")
    @GET
    @Path("/{gitProjectId}/pipelines/{pipelineId}/info")
    fun getPipeline(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "是否带有最新一次构建历史", required = false)
        @QueryParam("withHistory")
        withHistory: Boolean? = false
    ): Result<GitProjectPipeline?>

    @ApiOperation("开启或关闭Stream流水线")
    @POST
    @Path("/{gitProjectId}/pipelines/{pipelineId}/enable")
    fun enablePipeline(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "是否启用该流水线", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>

    @ApiOperation("获取Stream流水线列表")
    @GET
    @Path("/{gitProjectId}/pipelines/listInfo")
    fun listPipelineNames(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long
    ): Result<List<GitProjectPipeline>>

    @ApiOperation("查看项目下的指定构建详情")
    @GET
    @Path("/{gitProjectId}/builds/detail")
    fun getLatestBuildDetail(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "pipelineId", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam(value = "buildId", required = false)
        @QueryParam("buildId")
        buildId: String?
    ): Result<GitCIModelDetail?>

    @ApiOperation("可选条件检索Stream构建历史")
    @GET
    @Path("/{gitProjectId}/builds/history")
    fun getHistoryBuildList(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("查询开始时间，格式yyyy-MM-dd HH:mm:ss", required = false)
        @QueryParam("startBeginTime")
        startBeginTime: String?,
        @ApiParam("查询结束时间，格式yyyy-MM-dd HH:mm:ss", required = false)
        @QueryParam("endBeginTime")
        endBeginTime: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条（最大50）", required = false, defaultValue = "20", allowableValues = "range[1, 50]")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("分支", required = false)
        @QueryParam("branch")
        branch: String?,
        @ApiParam("源仓库ID", required = false)
        @QueryParam("sourceGitProjectId")
        sourceGitProjectId: Long?,
        @ApiParam("触发人", required = false)
        @QueryParam("triggerUser")
        triggerUser: String?,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Page<GitCIBuildHistory>>

    @ApiOperation("开启，关闭，初始化呢工蜂CI")
    @POST
    @Path("/{gitProjectId}/enable")
    fun enableGitCI(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("开启或关闭", required = true)
        @QueryParam("enabled")
        enabled: Boolean,
        @ApiParam("工蜂项目信息(初始化时用)", required = false)
        projectInfo: GitCIProjectInfo
    ): Result<Boolean>

    @ApiOperation("查询工蜂CI项目配置")
    @GET
    @Path("/{gitProjectId}/settings/get")
    fun getGitCIConf(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String
    ): Result<GitCIBasicSetting?>

    @ApiOperation("保存工蜂CI配置")
    @POST
    @Path("/{gitProjectId}/settings/save")
    fun saveGitCIConf(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String,
        @ApiParam("工蜂项目配置", required = true)
        gitCIUpdateSetting: GitCIUpdateSetting
    ): Result<Boolean>

    @ApiOperation("校验改用户工蜂的stream项目信息与权限")
    @POST
    @Path("/validate")
    fun validateGitProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目URL", required = true)
        request: GitUserValidateRequest
    ): Result<GitUserValidateResult?>

    @ApiOperation("刷新项目启动人")
    @POST
    @Path("/{gitProjectId}/reset/oauth")
    fun updateEnableUser(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "目标授权人", required = true)
        @QueryParam("authUserId")
        authUserId: String
    ): Result<Boolean>

    @ApiOperation("获取工蜂项目与STREAM关联列表")
    @GET
    @Path("/{type}/list")
    fun getProjects(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目列表类型", required = false)
        @PathParam("type")
        type: GitCIProjectType?,
        @ApiParam("搜索条件，模糊匹配path,name", required = false)
        @QueryParam("search")
        search: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条（最大50）", required = false, defaultValue = "10", allowableValues = "range[1, 50]")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("排序条件", required = false)
        @QueryParam("orderBy")
        orderBy: GitCodeProjectsOrder?,
        @ApiParam("排序类型", required = false)
        @QueryParam("sort")
        sort: GitCodeBranchesSort?
    ): Result<List<ProjectCIInfo>>
}
