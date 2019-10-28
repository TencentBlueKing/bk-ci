package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.AtomCollaboratorCreateReq
import com.tencent.devops.store.pojo.AtomCollaboratorCreateResp
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_ATOM_COOPERATION"], description = "插件-插件协作")
@Path("/user/market/atom/cooperation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAtomCooperationResource {

    @ApiOperation("为插件添加协作者")
    @POST
    @Path("/collaborator")
    fun addAtomCollaborator(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("申请成为插件协作者请求报文体", required = true)
        atomCollaboratorCreateReq: AtomCollaboratorCreateReq
    ): Result<AtomCollaboratorCreateResp>
}