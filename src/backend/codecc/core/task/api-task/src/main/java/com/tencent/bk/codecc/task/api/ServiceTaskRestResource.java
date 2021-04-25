/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.CreateTaskConfigVO;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.devops.common.api.CommonPageVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.pojo.GongfengBaseInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TASK"}, description = "任务管理接口")
@Path("/service/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTaskRestResource {
    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    Result<TaskDetailVO> getTaskInfo(
            @ApiParam(value = "任务英文名", required = true)
            @QueryParam("nameEn")
                    String nameEn);

    @ApiOperation("获取任务已接入工具列表")
    @Path("/tools")
    @GET
    Result<TaskBaseVO> getTaskToolList(
            @ApiParam(value = "任务ID", required = true)
            @QueryParam("taskId")
                    long taskId);


    @ApiOperation("从流水线注册任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerPipelineTask(
            @ApiParam(value = "任务详细信息", required = true)
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("通过taskId获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoById(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );

    @ApiOperation("批量获取任务信息")
    @Path("/list")
    @POST
    Result<List<TaskBaseVO>> getTaskInfosByIds(
            @ApiParam(value = "任务ID清单", required = true)
                    List<Long> taskIds);

    @ApiOperation("通过taskid查询任务信息，不包含工具信息")
    @Path("/taskInfoWithoutTools/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoWithoutToolsByTaskId(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId);

    @ApiOperation("修改任务信息")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            @ApiParam(value = "任务修改信息", required = true)
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );

    @ApiOperation("停用任务")
    @Path("/{taskId}")
    @DELETE
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "停用原因", required = true)
            @QueryParam("disabledReason")
                    String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );

    @ApiOperation("停用任务")
    @Path("/pipeline/stop")
    @DELETE
    Result<Boolean> stopTaskByPipeline(
        @ApiParam(value = "流水线ID", required = true)
        @QueryParam(value = "pipelineId")
            String pipelineId,
        @ApiParam(value = "停用原因", required = true)
        @QueryParam("disabledReason")
            String disabledReason,
        @ApiParam(value = "当前用户", required = true)
        @QueryParam("userName")
            String userName
    );

    @ApiOperation("检查任务是否存在")
    @Path("/exists/{taskId}")
    @GET
    Result<Boolean> checkTaskExists(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );


    @ApiOperation("获取所有的基础工具信息")
    @Path("/tool/meta")
    @GET
    Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache();


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}")
    @GET
    Result<PipelineTaskVO> getPipelineTask(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );

    @ApiOperation("通过流水线ID批量获取任务ID")
    @Path("/queryTaskListByPipelineIds")
    @POST
    Result<Set<String>> queryTaskListByPipelineIds(
        @ApiParam(value = "流水线ID集合", required = true)
            Set<String> pipelineIds
    );

    @ApiOperation("获取任务清单")
    @Path("/tasks")
    @GET
    Result<TaskListVO> getTaskList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );

    @ApiOperation("获取任务清单")
    @Path("/gongfeng/url")
    @GET
    Result<String> getGongfengRepoUrl(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);


    @ApiOperation("根据bg id获取任务清单")
    @Path("/bgId/{bgId}")
    @GET
    Result<List<TaskBaseVO>> getTasksByBgId(
            @ApiParam(value = "事业群id", required = true)
            @PathParam("bgId")
                    Integer bgId);


    @ApiOperation("获取任务信息清单列表")
    @Path("/detail/list")
    @POST
    Result<TaskListVO> getTaskDetailList(
            @ApiParam(value = "任务批量查询模型", required = true)
                    QueryTaskListReqVO queryTaskListReqVO);


    @ApiOperation("根据作者获取对应任务信息列表")
    @Path("/myTasks")
    @POST
    Result<Page<TaskInfoVO>> getTasksByAuthor(
            @ApiParam(value = "查询作者名下的任务列表", required = true)
                    QueryMyTasksReqVO reqVO);


    @ApiOperation("修改流水线CodeCC配置的规则集")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/tasks/{taskId}/checkerSets")
    @PUT
    Result<Boolean> updatePipelineTaskCheckerSets(
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "项目ID", required = true)
            @PathParam(value = "projectId")
                    String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "修改规则集列表", required = true)
                    UpdateCheckerSet2TaskReqVO updateCheckerSet2TaskReqVO
    );

    @ApiOperation("获取工蜂项目信息Map")
    @Path("/gongfeng/info")
    @POST
    Result<Map<Integer, GongfengPublicProjVO>> getGongfengProjInfo(
            @ApiParam(value = "工蜂项目ID集合", required = true)
                    Collection<Integer> gfProjectId
    );


    @ApiOperation("获取工蜂项目信息Map")
    @Path("/gongfeng/sync/bgId/{bgId}")
    @GET
    Result<Boolean> syncGongfengStatProj(
            @ApiParam(value = "事业群ID", required = true)
            @PathParam(value = "bgId")
                    Integer bgId
    );

    @ApiOperation("获取工蜂项目度量信息Map")
    @Path("/gongfeng/stat/bgId/{bgId}")
    @POST
    Result<Map<Integer, ProjectStatVO>> getGongfengStatProjInfo(
            @ApiParam(value = "事业群ID", required = true)
            @PathParam(value = "bgId")
                    Integer bgId,
            @ApiParam(value = "工蜂项目ID集合", required = true)
                    Collection<Integer> gfProjectId
    );

    @ApiOperation("获取工蜂项目基本信息")
    @Path("/gongfeng/base")
    @GET
    Result<GongfengBaseInfo> getGongfengBaseInfo(
            @ApiParam(value = "事业群ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);

    @ApiOperation("按事业群ID获取部门ID集合")
    @Path("/org/bgId/{bgId}")
    @GET
    Result<Set<Integer>> queryDeptIdByBgId(
            @ApiParam(value = "事业群ID", required = true)
            @PathParam(value = "bgId")
                    Integer bgId
    );

    @ApiOperation("多条件批量获取任务详情列表")
    @Path("/batch/list")
    @POST
    Result<List<TaskDetailVO>> batchGetTaskList(
            @ApiParam(value = "任务批量查询模型", required = true)
                    QueryTaskListReqVO queryTaskListReqVO);

    @ApiOperation("路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId
    );

    @ApiOperation("手动触发个性化流水线")
    @Path("/custom/pipeline")
    @POST
    Result<TriggerPipelineOldRsp> triggerCustomPipeline(
            @ApiParam(value = "触发参数", required = true)
                    TriggerPipelineOldReq triggerPipelineReq,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId);


    @ApiOperation("手动触发个性化流水线(新版本)")
    @Path("/custom/pipeline/new")
    @POST
    Result<TriggerPipelineRsp> triggerCustomPipelineNew(
            @ApiParam(value = "触发参数", required = true)
                    TriggerPipelineReq triggerPipelineReq,
            @ApiParam(value = "应用code", required = true)
            @QueryParam("appCode")
                    String appCode,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId);


    @ApiOperation("批量获取个性化扫描任务列表")
    @Path("/custom/list")
    @POST
    Result<Page<CustomProjVO>> batchGetCustomTaskList(
            @ApiParam(value = "批量查询参数", required = true)
                    QueryTaskListReqVO reqVO);


    @ApiOperation("分页查询任务列表")
    @Path("/detail/page")
    @POST
    Result<Page<TaskDetailVO>> getTaskDetailPage(
            @ApiParam(value = "批量查询参数", required = true)
                    QueryTaskListReqVO reqVO);

    @ApiOperation("分页查询任务列表")
    @Path("/author/taskId/{taskId}")
    @PUT
    Result<Boolean> authorTransfer(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "作者转换信息", required = true)
            List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId);

    @ApiOperation("获取蓝盾插件开源扫描任务ID")
    @Path("/bkPlugin/taskId/list")
    @POST
    Result<List<Long>> getBkPluginTaskIds();

    @ApiOperation("获取工蜂任务信息")
    @Path("/taskId/gongfengProj/list")
    @POST
    Result<Map<Long, GongfengPublicProjVO>> getGongfengProjInfoByTaskId(
            @ApiParam(value = "任务ID集合", required = true)
                    List<Long> taskId
    );

    @ApiOperation("获取工蜂任务信息")
    @Path("/aliasName/gongfengProj/taskInfo")
    @GET
    Result<TaskDetailVO> getTaskInfoByAliasName(
            @ApiParam(value = "代码库别名", required = true)
            @HeaderParam("aliasName")
                    String aliasName
    );

    @ApiOperation("获取工蜂任务信息")
    @Path("/id/gongfengProj/taskInfo")
    @GET
    Result<TaskDetailVO> getTaskInfoByGongfengId(
            @ApiParam(value = "代码库别名", required = true)
            @HeaderParam("id")
                    Integer id
    );

    @ApiOperation("获取工蜂CI项目基本信息")
    @Path("/gongfengci/{gongfengId}/base")
    @GET
    Result<GongfengBaseInfo> getGongfengCIBaseInfo(
            @ApiParam(value = "工蜂ID", required = true)
            @PathParam("gongfengId")
                    Integer gongfengId);

    @ApiOperation("创建开源扫描任务")
    @Path("/repo/create")
    @POST
    Result<Boolean> createTaskForBkPlugins(
            @ApiParam(value = "代码库别名", required = true)
            @HeaderParam("repoId")
                    String repoId,
            @ApiParam(value = "语言集", required = true)
                    CreateTaskConfigVO createTaskConfigVO
    );

    @ApiOperation("按创建来源查询任务ID列表")
    @Path("/query/by/createFrom")
    @POST
    Result<List<Long>> queryTaskIdByCreateFrom(
            @ApiParam(value = "任务ID集合", required = true)
                    List<String> createFrom
    );

    @ApiOperation("获取任务信息")
    @Path("/get")
    @GET
    Result<TaskDetailVO> getTaskInfoWithoutToolsByStreamName(
            @ApiParam(value = "任务英文名", required = true)
            @QueryParam("nameEn")
                    String nameEn
    );

    @ApiOperation("停止api触发流水线")
    @Path("/custom/pipeline/codeccBuildId/{codeccBuildId}")
    @DELETE
    Result<Boolean> stopRunningApiTask(
            @ApiParam(value = "codecc构建id", required = true)
            @PathParam("codeccBuildId")
            String codeccBuildId,
            @ApiParam(value = "应用code", required = true)
            @QueryParam("appCode")
                    String appCode,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId);
}
