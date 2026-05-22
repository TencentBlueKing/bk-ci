package com.tencent.devops.common.web.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * 验证用户态接口调用,在auth服务实现
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/service/userApi/auth/permission")
@ServiceInterface("auth")
interface ServiceUserApiAuthPermissionResource {

    /**
     * 检查是否有项目访问权限
     */
    @Path("checkVisitPermission")
    @GET
    fun checkVisitPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @QueryParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<Boolean>
}
