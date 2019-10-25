package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.enums.OpSortTypeEnum
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

@Api(tags = ["OP_PIPELINE_ATOM"], description = "OP-流水线-原子")
@Path("/op/pipeline/atom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAtomResource {

    @ApiOperation("添加流水线原子")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线原子请求报文体", required = true)
        atomCreateRequest: AtomCreateRequest
    ): Result<Boolean>

    @ApiOperation("更新流水线原子信息")
    @PUT
    @Path("/{id}")
    fun update(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("流水线原子ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "流水线原子请求报文体", required = true)
        atomUpdateRequest: AtomUpdateRequest
    ): Result<Boolean>

    @ApiOperation("获取所有流水线原子信息")
    @GET
    @Path("/")
    fun listAllPipelineAtoms(
        @ApiParam("原子名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("原子类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
        @QueryParam("atomType")
        atomType: AtomTypeEnum?,
        @ApiParam("支持的服务范围（pipeline/quality/all 分别表示流水线/质量红线/全部）", required = false)
        @QueryParam("serviceScope")
        serviceScope: String?,
        @ApiParam("操作系统（ALL/WINDOWS/LINUX/MACOS）", required = false)
        @QueryParam("os")
        os: String?,
        @ApiParam("原子所属范畴，TRIGGER：触发器类原子 TASK：任务类原子", required = false)
        @QueryParam("category")
        category: String?,
        @ApiParam("原子分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @ApiParam("插件状态", required = false)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: OpSortTypeEnum? = OpSortTypeEnum.updateTime,
        @ApiParam("排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<AtomResp<Atom>?>

    @ApiOperation("根据ID获取流水线原子信息")
    @GET
    @Path("/{id}")
    fun getPipelineAtomById(
        @ApiParam("流水线原子ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Atom?>

    @ApiOperation("根据ID获取流水线原子信息")
    @DELETE
    @Path("/{id}")
    fun deletePipelineAtomById(
        @ApiParam("流水线原子ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>

    @ApiOperation("审核原子")
    @Path("/{atomId}/approve")
    @PUT
    fun approveAtom(
        @ApiParam("原子ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @ApiParam("审核原子请求报文")
        approveReq: ApproveReq
    ): Result<Boolean>

    @ApiOperation("审核可见范围")
    @PUT
    @Path("/{atomCode}/visible/approve/")
    fun approveVisibleDept(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("原子标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("可见范围审核请求报文", required = true)
        visibleApproveReq: VisibleApproveReq
    ): Result<Boolean>

    @ApiOperation("查看原子可见范围")
    @GET
    @Path("/{atomCode}/visible")
    fun getVisibleDept(
        @ApiParam("原子代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<StoreVisibleDeptResp?>

    @ApiOperation("把项目迁移到指定项目组下")
    @PUT
    @Path("/repositories/git/move/codes/{atomCode}/group")
    fun moveGitProjectToGroup(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("原子插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam(value = "项目组代码", required = false)
        @QueryParam("groupCode")
        groupCode: String?
    ): Result<Boolean>
}