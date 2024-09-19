package com.tencent.devops.remotedev.api.op

import com.tencent.devops.remotedev.pojo.ClientTipsInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.project.pojo.Result

@Tag(name = "OP_CLIENT_TIPS", description = "OP_CLIENT_TIPS")
@Path("/op/clientips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpClientTipsResource {
    @Operation(summary = "新增或修改tips")
    @POST
    @Path("/create_or_update")
    fun createOrUpdate(
        @Parameter(description = "有ID就是修改，没有就是新增", required = false)
        @QueryParam("id")
        id: Long?,
        data: ClientTipsInfo
    ): Result<Boolean>

    @Operation(summary = "删除tips，支持批量")
    @DELETE
    @Path("/delete")
    fun deleteTips(
        ids: Set<Long>
    ): Result<Boolean>
}