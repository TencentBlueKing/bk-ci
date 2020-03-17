package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.OpEditInfoDTO
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
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
        @ApiParam("是否审核中", required = false)
        @QueryParam("isApprove")
        isApprove: Boolean?,
        @ApiParam("是否推荐", required = false)
        @QueryParam("isRecommend")
        isRecommend: Boolean?,
        @ApiParam("是否公共", required = false)
        @QueryParam("isPublic")
        isPublic: Boolean?,
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
    @Path("/serviceIds/{serviceId}")
    fun getExtsionServiceById(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务ID", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<ServiceVersionVO?>

    @ApiOperation("编辑扩展服务")
    @POST
    @Path("/serviceIds/{serviceId}/serviceCodes/{serviceCode}")
    fun editExtService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务ID", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam("扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("修改信息", required = true)
        updateInfo: OpEditInfoDTO
    ): Result<Boolean>

    @ApiOperation("根据ID获取扩展服务信息")
    @GET
    @Path("/serviceCodes/{serviceCode}")
    fun getExtsionServiceByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务ID", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<ServiceVersionVO?>

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

    @ApiOperation("下架扩展服务")
    @PUT
    @Path("/{serviceCode}/offline/")
    fun offlineService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceCode", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("下架请求报文")
        serviceOffline: ServiceOfflineDTO
    ): Result<Boolean>

    @ApiOperation("审核可见范围")
    @PUT
    @Path("/{serviceCode}/visible/approve/")
    fun approveVisibleDept(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展标识", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("可见范围审核请求报文", required = true)
        visibleApproveReq: VisibleApproveReq
    ): Result<Boolean>

    @ApiOperation("删除扩展服务")
    @DELETE
    @Path("/serviceIds/{serviceId}")
    fun deleteAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展Id", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @ApiOperation("查看可见范围")
    @GET
    @Path("/{serviceCode}/visible")
    fun getVisibleDept(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("代码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("删除扩展服务可见范围")
    @DELETE
    @Path("/{serviceCode}")
    fun deleteVisibleDept(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("机构Id集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("deptIds")
        deptIds: String
    ): Result<Boolean>
}