package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_SERVICE"], description = "服务扩展--基础信息")
@Path("/user/market/extension")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceResource {
    @ApiOperation("获取服务扩展市场首页的数据")
    @Path("/service/list/main")
    @GET
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<ExtServiceMainItemVo>>

    @ApiOperation("服务扩展市场搜索服务扩展")
    @GET
    @Path("/service/list/")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("服务扩展名称", required = false)
        @QueryParam("serviceName")
        serviceName: String?,
        @ApiParam("服务扩展分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: ExtServiceSortTypeEnum? = ExtServiceSortTypeEnum.CREATE_TIME,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SearchExtServiceVO>
}