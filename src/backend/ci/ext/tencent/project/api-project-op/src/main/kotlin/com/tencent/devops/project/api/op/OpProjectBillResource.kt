package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OP_PROJECT_COST_ALLOCATION", description = "OP-项目成本分摊相关接口")
@Path("/op/cost/allocation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectBillResource {
    @Operation(summary = "检查项目活跃度--项目列表")
    @POST
    @Path("/processInactiveProject/")
    fun processInactiveProject(
        @Parameter(description = "项目ID列表", required = true)
        projectList: List<String>
    ): Result<Boolean>

    @Operation(summary = "检查项目活跃度--按条件")
    @POST
    @Path("/processInactiveProjectByCondition/")
    fun processInactiveProjectByCondition(
        @Parameter(description = "条件", required = true)
        projectConditionDTO: ProjectConditionDTO
    ): Result<Boolean>

    @Operation(summary = "检查项目是否关联运营产品--按条件")
    @POST
    @Path("/checkProjectRelatedProduct/")
    fun checkProjectRelatedProduct(
        @Parameter(description = "项目ID列表", required = true)
        projectList: List<String>
    ): Result<Boolean>

    @Operation(summary = "同步OBS数据")
    @POST
    @Path("/syncOperationalProduct/")
    fun syncOperationalProduct(): Result<Boolean>

    @Operation(summary = "上报货币化数据")
    @POST
    @Path("/reportBillsData/{yearAndMonthOfReportStr}")
    fun reportBillsData(
        @Parameter(description = "账单日期.格式如202403,即上报2024年3月份账单", required = true)
        @PathParam("yearAndMonthOfReportStr")
        yearAndMonthOfReportStr: String
    ): Result<Boolean>
}
