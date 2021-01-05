package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.op.GitCiMarketAtom
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_GITCI_ATOM"], description = "OP-GitCI-插件")
@Path("/op/gitci/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitCiMarketAtomResource {

    @ApiOperation("获取工蜂CI支持的插件列表")
    @GET
    @Path("/list")
    fun list(): Result<List<GitCiMarketAtom>?>

    @ApiOperation("新增工蜂CI支持的插件")
    @POST
    @Path("/")
    fun add(
        gitCiMarketAtom: GitCiMarketAtom
    ): Result<Boolean>

    @ApiOperation("删除工蜂CI支持的插件")
    @DELETE
    @Path("/{atomCode}")
    fun delete(
        @ApiParam("插件Code", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>
}