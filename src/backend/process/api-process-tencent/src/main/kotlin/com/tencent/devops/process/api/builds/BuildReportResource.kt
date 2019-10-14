package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_VM_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.report.ReportEmail
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
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

@Api(tags = ["BUILD_REPORT"], description = "构建-自定义报告")
@Path("/build/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildReportResource {
    @ApiOperation("创建自定义报告")
    @Path("/elements/{elementId}")
    @POST
    fun create(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String,
        @ApiParam(value = "构建机名称", required = true)
        @PathParam("elementId")
        elementId: String,
        @ApiParam(value = "构建机名称", required = true)
        @QueryParam("indexFile")
        indexFile: String,
        @ApiParam(value = "报告名称", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam(value = "报告类型", required = true)
        @QueryParam("reportType")
        reportType: ReportTypeEnum,
        @ApiParam(value = "报告邮件", required = false)
        reportEmail: ReportEmail?
    ): Result<Boolean>

    @ApiOperation("获取自定义报告根目录Url")
    @Path("/elements/{elementId}/rootUrl")
    @GET
    fun getRootUrl(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建机名称", required = true)
        @PathParam("elementId")
        elementId: String
    ): Result<String>
}