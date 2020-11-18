package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_PLATFORM
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.experience.pojo.download.CheckVersionParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_EXPERIENCE_DOWNLOAD"], description = "版本体验-下载管理")
@Path("/app/experiences/download")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceDownloadResource {
    @ApiOperation("检查更新")
    @Path("/checkVersion")
    @POST
    fun checkVersion(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("平台", required = true)
        @HeaderParam(AUTH_HEADER_PLATFORM)
        platform: Int,
        @ApiParam("检查更新参数", required = true)
        params: List<CheckVersionParam>
    )
}
