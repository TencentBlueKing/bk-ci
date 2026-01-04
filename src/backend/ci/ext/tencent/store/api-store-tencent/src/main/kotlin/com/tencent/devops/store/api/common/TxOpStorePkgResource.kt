import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import jakarta.ws.rs.DefaultValue

@Tag(name = "OP_STORE_PKG", description = "OP-研发商店包文件管理")
@Path("/op/store/pkg")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TxOpStorePkgResource {

    @Operation(summary = "批量更新包文件SHA256值")
    @POST
    @Path("/updateSha256")
    fun updatePackageSha256(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        @DefaultValue("100")
        pageSize: Int
    ): Result<Boolean>
}