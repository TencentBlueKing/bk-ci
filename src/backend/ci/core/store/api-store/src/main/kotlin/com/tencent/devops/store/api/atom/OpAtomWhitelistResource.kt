package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.AtomWhitelist
import com.tencent.devops.store.pojo.common.AtomWhitelistCreateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_ATOM_WHITELIST", description = "OP-插件功能白名单管理")
@Path("/op/atom/whitelist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAtomWhitelistResource {

    @Operation(summary = "新增或更新插件功能白名单记录")
    @POST
    @Path("/types/{whitelistType}/addOrUpdate")
    fun addOrUpdateWhitelist(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "白名单类型", required = true)
        @PathParam("whitelistType")
        whitelistType: String,
        @Parameter(description = "白名单创建请求", required = true)
        request: AtomWhitelistCreateRequest
    ): Result<Boolean>

    @Operation(summary = "根据白名单类型删除插件功能白名单记录")
    @DELETE
    @Path("/types/{whitelistType}")
    fun deleteWhitelist(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "白名单类型", required = true)
        @PathParam("whitelistType")
        whitelistType: String
    ): Result<Boolean>

    @Operation(summary = "启用或禁用插件功能白名单记录")
    @PUT
    @Path("/types/{whitelistType}/status")
    fun updateWhitelistStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "白名单类型", required = true)
        @PathParam("whitelistType")
        whitelistType: String,
        @Parameter(description = "是否启用", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>

    @Operation(summary = "分页查询插件功能白名单记录")
    @GET
    @Path("/list")
    fun listWhitelists(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "白名单类型", required = false)
        @QueryParam("whitelistType")
        whitelistType: String? = null,
        @Parameter(description = "是否启用", required = false)
        @QueryParam("enabled")
        enabled: Boolean? = null,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页大小", required = false)
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        @QueryParam("pageSize")
        pageSize: Int = 10
    ): Result<Page<AtomWhitelist>>
}