package com.tencent.devops.dispatch.windows.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.windows.pojo.VMType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "USER_SYSTEM_VERSION", description = "USER接口-系统版本资源")
@Path("user/windows/systemVersions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSystemVersionResource {
    @GET
    @Path("/")
    @Operation(summary = "获取windows系统版本列表")
    fun list(): Result<List<VMType>>
}
