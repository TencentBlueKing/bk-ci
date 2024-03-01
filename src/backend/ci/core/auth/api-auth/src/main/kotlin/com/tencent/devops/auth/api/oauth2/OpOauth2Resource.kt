package com.tencent.devops.auth.api.oauth2

import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.dto.ScopeOperationDTO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result

@Tag(name = "OP_OAUTH2", description = "oauth2相关-op接口")
@Path("/op/oauth2/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpOauth2Resource {
    @POST
    @Path("/createClientDetails")
    @Operation(summary = "新增Oauth2客户端信息")
    fun createClientDetails(
        @Parameter(description = "Oauth2客户端请求实体", required = true)
        clientDetailsDTO: ClientDetailsDTO
    ): Result<Boolean>

    @DELETE
    @Path("/deleteClientDetails")
    @Operation(summary = "删除Oauth2客户端信息")
    fun deleteClientDetails(
        @Parameter(description = "客户端ID", required = true)
        @QueryParam("clientId")
        clientId: String
    ): Result<Boolean>

    @POST
    @Path("/createScopeOperation")
    @Operation(summary = "新增Oauth2授权操作信息")
    fun createScopeOperation(
        @Parameter(description = "Oauth2授权操作信息请求实体", required = true)
        scopeOperationDTO: ScopeOperationDTO
    ): Result<Boolean>

    @DELETE
    @Path("/deleteScopeOperation")
    @Operation(summary = "删除Oauth2授权操作信息")
    fun deleteScopeOperation(
        @Parameter(description = "授权操作ID", required = true)
        @QueryParam("operationId")
        operationId: String
    ): Result<Boolean>
}
