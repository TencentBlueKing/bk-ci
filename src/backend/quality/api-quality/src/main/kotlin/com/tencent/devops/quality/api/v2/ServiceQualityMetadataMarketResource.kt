package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_METADATA_MARKET"], description = "服务-质量红线-插件市场")
@Path("/service/metadata/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityMetadataMarketResource {

    @ApiOperation("注册插件指标的元数据")
    @Path("/setMetadata")
    @POST
    fun setTestMetadata(
        @QueryParam("userId")
        userId: String,
        @QueryParam("atomCode")
        atomCode: String,
        metadataList: List<QualityMetaData>
    ): Result<Map<String/* dataId */, Long/* metadataId */>>

    @ApiOperation("刷新插件指标的元数据")
    @Path("/refreshMetadata")
    @PUT
    fun refreshMetadata(
        @QueryParam("elementType")
        elementType: String
    ): Result<Map<String, String>>

    @ApiOperation("删除插件指标的测试元数据")
    @Path("/deleteTestMetadata")
    @DELETE
    fun deleteTestMetadata(
        @QueryParam("elementType")
        elementType: String
    ): Result<Int>
}