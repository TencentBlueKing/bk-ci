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
import com.tencent.devops.stream.pojo.openapi.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.openapi.GitCIBasicSetting
import com.tencent.devops.stream.pojo.openapi.GitCIProjectType
import com.tencent.devops.stream.pojo.openapi.GitCIUpdateSetting
import com.tencent.devops.stream.pojo.openapi.GitUserValidateRequest
import com.tencent.devops.stream.pojo.openapi.GitUserValidateResult
import com.tencent.devops.stream.pojo.openapi.ProjectCIInfo
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitCIModelDetail
import com.tencent.devops.stream.v1.pojo.V1GitProjectPipeline
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

    @ApiOperation(
        "人工TriggerBuild启动构建",
        tags = ["v3_stream_app_pipelines_startup", "v3_stream_user_pipelines_startup"]
    )
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

    @ApiOperation(
        "工蜂project转换为streamProject",
        tags = ["v3_stream_app_tranfer_projectname", "v3_stream_user_tranfer_projectname"]
    )
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

    @ApiOperation("项目下所有Stream流水线概览", tags = ["v3_stream_app_pipelines_list", "v3_stream_user_pipelines_list"])
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
    ): Result<Page<V1GitProjectPipeline>>

    @ApiOperation("获取指定Stream流水线信息", tags = ["v3_stream_app_pipelines_info", "v3_stream_user_pipelines_info"])
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
    ): Result<V1GitProjectPipeline?>

    @ApiOperation("开启或关闭Stream流水线", tags = ["v3_stream_app_pipelines_enable", "v3_stream_user_pipelines_enable"])
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

    @ApiOperation("获取Stream流水线列表", tags = ["v3_stream_app_pipelines_listInfo", "v3_stream_user_pipelines_listInfo"])
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
    ): Result<List<V1GitProjectPipeline>>

    @ApiOperation("查看项目下的指定构建详情", tags = ["v3_stream_app_builds_detail", "v3_stream_user_builds_detail"])
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
    ): Result<V1GitCIModelDetail?>

    @ApiOperation("可选条件检索Stream构建历史", tags = ["v3_stream_app_builds_history", "v3_stream_user_builds_history"])
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
    ): Result<Page<V1GitCIBuildHistory>>

    @ApiOperation("开启，关闭，初始化呢工蜂CI", tags = ["v3_stream_app_ci_enable", "v3_stream_user_ci_enable"])
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

    @ApiOperation("查询工蜂CI项目配置", tags = ["v3_stream_app_ci_settings_get", "v3_stream_user_ci_settings_get"])
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

    @ApiOperation("保存工蜂CI配置", tags = ["v3_stream_app_ci_update_setttings", "v3_stream_user_ci_update_setttings"])
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

    @ApiOperation(
        "校验改用户工蜂的stream项目信息与权限",
        tags = ["v3_stream_app_project_validate", "v3_stream_user_project_validate"]
    )
    @POST
    @Path("/validate")
    fun validateGitProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目URL", required = true)
        request: GitUserValidateRequest
    ): Result<GitUserValidateResult?>

    @ApiOperation("刷新项目启动人", tags = ["v3_stream_app_ci_reset_oauth", "v3_stream_user_ci_reset_oauth"])
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

    @ApiOperation("获取工蜂项目与STREAM关联列表", tags = ["v3_stream_user_stream_list", "v3_stream_app_stream_list"])
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
