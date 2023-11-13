package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_EXPERTSUPPORT"], description = "OP_EXPERTSUPPORT")
@Path("/op/expertSup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpExpertSupport {
    @ApiOperation("更新专家协助单据")
    @PUT
    @Path("/update")
    fun updateExpertSup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "更新数据")
        data: UpdateSupportData
    ): Result<Boolean>

    @ApiOperation("查询专家协助配置")
    @GET
    @Path("/fetchConfig")
    fun fetchSupportConfig(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("类型")
        @QueryParam("type")
        type: ExpertSupportConfigType
    ): Result<List<FetchExpertSupResp>>

    @ApiOperation("新增专家协助配置")
    @POST
    @Path("/addConfig")
    fun addSupConfig(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("创建数据")
        data: CreateExpertSupportConfigData
    ): Result<Boolean>

    @ApiOperation("删除专家协助配置")
    @DELETE
    @Path("/deleteConfig")
    fun deleteConfig(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("id")
        id: Long
    )
}
