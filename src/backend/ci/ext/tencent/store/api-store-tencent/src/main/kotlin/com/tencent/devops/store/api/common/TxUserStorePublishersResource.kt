package com.tencent.devops.store.api.common

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STORE_PUBLISHERS", description = "研发商店-发布者")
@Path("/user/store/publishers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TxUserStorePublishersResource {
    @Operation(summary = "矫正组件的首次发布人")
    @PUT
    @Path("/updateComponentFirstPublisher")
    fun updateComponentFirstPublisher(
        @Parameter(description = "组件类型", required = true)
        @QueryParam("storeType")
        type: StoreTypeEnum
    )
}
