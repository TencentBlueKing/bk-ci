package com.tencent.devops.openapi.api.apigw.desktop

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.DEVX_HEADER_GW_TOKEN
import com.tencent.devops.common.api.auth.DEVX_HEADER_NGGW_CLIENT_ADDRESS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OPEN_API_DESKTOP", description = "云桌面SDK API")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/desktop")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDeskTopResource {
    @Operation(summary = "云桌面SDK根据X-BK-NGGW-CLIENT-ADDRESS获取云桌面信息", tags = ["v4_app_desktop_workspace_detail"])
    @GET
    @Path("/project/workspace/detail")
    fun getProjectWorkspace(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "IP", required = false)
        @HeaderParam(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        @BkField(patternStyle = BkStyleEnum.IP_STYLE, required = true, message = "need ipv4")
        desktopIP: String,
        @Parameter(description = "devx token", required = false)
        @HeaderParam(DEVX_HEADER_GW_TOKEN)
        @BkField(minLength = 32, maxLength = 32, required = true, message = "need token")
        devxGwToken: String
    ): Result<WeSecProjectWorkspace?>
}
