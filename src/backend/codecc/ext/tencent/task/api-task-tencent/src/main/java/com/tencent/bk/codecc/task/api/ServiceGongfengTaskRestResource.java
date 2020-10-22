package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.auth.pojo.GongfengBaseInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.CODECC_AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 工蜂任务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TASK"}, description = "工蜂任务管理接口")
@Path("/service/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceGongfengTaskRestResource {

    @ApiOperation("获取任务清单")
    @Path("/gongfeng/url")
    @GET
    CodeCCResult<String> getGongfengRepoUrl(
        @ApiParam(value = "任务id", required = true)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId);

    @ApiOperation("获取工蜂项目基本信息")
    @Path("/gongfeng/base")
    @GET
    CodeCCResult<GongfengBaseInfo> getGongfengBaseInfo(
        @ApiParam(value = "事业群ID", required = true)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId);


    @ApiOperation("获取工蜂项目信息Map")
    @Path("/gongfeng/info")
    @POST
    CodeCCResult<Map<Integer, GongfengPublicProjVO>> getGongfengProjInfo(
        @ApiParam(value = "工蜂项目ID集合", required = true)
            Collection<Integer> gfProjectId
    );


    @ApiOperation("获取工蜂项目信息Map")
    @Path("/gongfeng/sync/bgId/{bgId}")
    @GET
    CodeCCResult<Boolean> syncGongfengStatProj(
        @ApiParam(value = "事业群ID", required = true)
        @PathParam(value = "bgId")
            Integer bgId
    );

    @ApiOperation("获取工蜂项目度量信息Map")
    @Path("/gongfeng/stat/bgId/{bgId}")
    @POST
    CodeCCResult<Map<Integer, ProjectStatVO>> getGongfengStatProjInfo(
        @ApiParam(value = "事业群ID", required = true)
        @PathParam(value = "bgId")
            Integer bgId,
        @ApiParam(value = "工蜂项目ID集合", required = true)
            Collection<Integer> gfProjectId
    );

    @ApiOperation("获取工蜂任务信息")
    @Path("/taskId/gongfengProj/list")
    @POST
    CodeCCResult<Map<Long, GongfengPublicProjVO>> getGongfengProjInfoByTaskId(
        @ApiParam(value = "任务ID集合", required = true)
            List<Long> taskId
    );


    @ApiOperation("手动触发个性化流水线(新版本)")
    @Path("/custom/pipeline/new")
    @POST
    CodeCCResult<TriggerPipelineRsp> triggerCustomPipelineNew(
        @ApiParam(value = "触发参数", required = true)
            TriggerPipelineReq triggerPipelineReq,
        @ApiParam(value = "应用code", required = true)
        @QueryParam("appCode")
            String appCode,
        @ApiParam(value = "用户", required = true)
        @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
            String userId);


    @ApiOperation("批量获取个性化扫描任务列表")
    @Path("/custom/list")
    @POST
    CodeCCResult<Page<CustomProjVO>> batchGetCustomTaskList(
        @ApiParam(value = "批量查询参数", required = true)
            QueryTaskListReqVO reqVO);

    @ApiOperation("获取工蜂CI项目基本信息")
    @Path("/gongfengci/{gongfengId}/base")
    @GET
    CodeCCResult<GongfengBaseInfo> getGongfengCIBaseInfo(
        @ApiParam(value = "工蜂ID", required = true)
        @PathParam("gongfengId")
            Integer gongfengId);

    @ApiOperation("获取蓝盾插件开源扫描任务ID")
    @Path("/bkPlugin/taskId/list")
    @POST
    CodeCCResult<List<Long>> getBkPluginTaskIds();
}
