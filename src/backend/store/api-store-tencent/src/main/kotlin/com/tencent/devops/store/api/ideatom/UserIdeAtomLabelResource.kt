package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Label
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IDE_ATOM_LABEL"], description = "IDE插件-IDE插件标签")
@Path("/user/market/ideAtom/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserIdeAtomLabelResource {

    @ApiOperation("获取所有IDE插件标签信息")
    @GET
    @Path("/labels")
    fun getAllAtomLabels(): Result<List<Label>?>

    @ApiOperation("根据IDE插件ID获取IDE插件标签信息")
    @GET
    @Path("/atomIds/{atomId}/labels")
    fun getAtomLabelsByAtomId(
        @ApiParam("IDE插件ID", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<List<Label>?>
}