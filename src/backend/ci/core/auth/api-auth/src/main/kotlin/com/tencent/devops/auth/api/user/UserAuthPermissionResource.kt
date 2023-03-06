import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PERMISSION"], description = "用户-权限校验")
@Path("/user/auth/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthPermissionResource {
    @POST
    @Path("/validate/batch")
    @ApiOperation("批量校验用户是否拥有某个资源实例的操作")
    fun batchValidateUserResourcePermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @ApiParam("权限批量校验实体", required = true)
        permissionBatchValidateDTO : PermissionBatchValidateDTO
    ): Result<Map<String, Boolean>>
}
