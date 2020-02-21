package com.tencent.devops.store.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE_SERVICE"], description = "OP-流水线-扩展服务")
@Path("/op/pipeline/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpServiceResource {


    @ApiOperation("获取扩展服务信息")
    @GET
    @Path("/")
    fun listAllExtsionServices(
        @ApiParam("扩展服务名称", required = false)
        @QueryParam("serviceName")
        serviceName: String?,
        @ApiParam("扩展点ID", required = false)
        @QueryParam("itemId")
        itemId: String?,
        @ApiParam("标签ID", required = false)
        @QueryParam("lableId")
        lableId: String?,
        @ApiParam("扩展服务状态", required = false)
        @QueryParam("serviceStatus")
        serviceStatus: ExtServiceStatusEnum?,
        @ApiParam("是否推荐", required = false)
        @QueryParam("isRecommend")
        isRecommend: Int?,
        @ApiParam("是否公共", required = false)
        @QueryParam("isPublic")
        isPublic: Int?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: OpSortTypeEnum? = OpSortTypeEnum.UPDATE_TIME,
        @ApiParam("排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ExtServiceInfoResp?>

    @ApiOperation("根据ID获取扩展服务信息")
    @GET
    @Path("/{serviceId}")
    fun getPipelineServiceById(
        @ApiParam("扩展服务ID", required = true)
        @QueryParam("serviceId")
        serviceId: String
    ): Result<ExtensionServiceVO?>


    @ApiOperation("审核扩展服务")
    @Path("/{serviceId}/approve")
    @PUT
    fun approveService(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展ID", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam("审核扩展服务请求报文")
        approveReq: ServiceApproveReq
    ): Result<Boolean>
}