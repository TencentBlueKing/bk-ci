package com.tencent.devops.openapi.api.apigw.v4.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OPENAPI_JOB_V4", description = "OPENAPI-JOB")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwJobResourceV4 {
    @Operation(summary = "批量写入display_name的接口", tags = ["v4_app_job_write_display_name"])
    @POST
    @Path("/stock_data_update/write_display_name")
    fun writeDisplayName(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )

    @Operation(summary = "蓝盾agent状态版本更新接口", tags = ["v4_app_job_update_devops_agent"])
    @POST
    @Path("/stock_data_update/update_devops_agent")
    fun updateDevopsAgent(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE
    )
}
