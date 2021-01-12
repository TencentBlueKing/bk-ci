package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.op.GitCiMarketAtom
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
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

    @ApiOperation("批量新增工蜂CI支持的插件")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "插件信息列表", required = true)
        gitCiMarketAtomReq: GitCiMarketAtomReq
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
