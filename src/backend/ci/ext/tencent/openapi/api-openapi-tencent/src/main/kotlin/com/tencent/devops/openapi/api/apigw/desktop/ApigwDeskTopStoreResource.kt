package com.tencent.devops.openapi.api.apigw.desktop

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPEN_API_DESKTOP_STORE", description = "云桌面研发商店 API")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/desktop/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDeskTopStoreResource {

    @Operation(summary = "获取组件分类信息列表", tags = ["v4_app_desktop_store_classify_list"])
    @GET
    @Path("/classifies/types/{storeType}/list")
    fun getClassifyList(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum
    ): Result<List<Classify>>
}
