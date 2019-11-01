package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.JinGangBugCount
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_JIN_GANG"], description = "服务-金刚app扫描任务")
@Path("/service/jingang")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceJinGangAppResource {

    @ApiOperation("启动金刚扫描")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/app/scan")
    fun scanApp(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线构建id", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("流水线构建No", required = true)
        @QueryParam("buildNo")
        buildNo: Int,
        @ApiParam("element ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @ApiParam("文件路径", required = true)
        @QueryParam("file")
        file: String,
        @ApiParam("是否是自定义仓库", required = true)
        @QueryParam("isCustom")
        isCustom: Boolean,
        @ApiParam("运行类型（3表示中跑静态，1表示跑静态和跑动态）", required = true)
        @QueryParam("runType")
        runType: String
    ): Result<String>

    @ApiOperation("获取金刚安全漏洞数量")
    @Path("/bug/count")
    @POST
    fun countBug(
        @ApiParam("项目ID集合", required = false)
        projectIds: Set<String>? = setOf()
    ): Result<Map<String/* projectId */, JinGangBugCount>>

    @ApiOperation("获取金刚安全警告数量")
    @Path("/risk/count")
    @POST
    fun countRisk(
        @ApiParam("项目ID集合", required = false)
        projectIds: Set<String>? = setOf()
    ): Result<Map<String/* projectId */, Int>>
}