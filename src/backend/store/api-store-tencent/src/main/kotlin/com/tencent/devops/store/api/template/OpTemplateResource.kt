package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.ApproveReq
import com.tencent.devops.store.pojo.template.OpTemplateResp
import com.tencent.devops.store.pojo.template.enums.OpTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateTypeEnum
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

@Api(tags = ["OP_PIPELINE_TEMPLATE"], description = "OP-流水线-模版")
@Path("/op/pipeline/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTemplateResource {

    @ApiOperation("获取市场模版")
    @GET
    @Path("/list")
    fun listTemplates(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @ApiParam("模版状态", required = false)
        @QueryParam("templateStatus")
        templateStatus: TemplateStatusEnum?,
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateTypeEnum?,
        @ApiParam("模版分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("应用范畴", required = false)
        @QueryParam("category")
        category: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("是否最新", required = false)
        @QueryParam("latestFlag")
        latestFlag: Boolean?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: OpTemplateSortTypeEnum ? = OpTemplateSortTypeEnum.UpdateTime,
        @ApiParam("排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<OpTemplateResp>

    @ApiOperation("审核模版")
    @Path("/{templateId}/approve")
    @PUT
    fun approveTemplate(
        @ApiParam("ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("审核模版请求报文")
        approveReq: ApproveReq
    ): Result<Boolean>

    @ApiOperation("审核可见范围")
    @PUT
    @Path("/{templateCode}/visible/approve/")
    fun approveVisibleDept(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("标识", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @ApiParam("可见范围审核请求报文", required = true)
        visibleApproveReq: VisibleApproveReq
    ): Result<Boolean>

    @ApiOperation("查看可见范围")
    @GET
    @Path("/{templateCode}/visible")
    fun getVisibleDept(
        @ApiParam("代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<StoreVisibleDeptResp?>
}