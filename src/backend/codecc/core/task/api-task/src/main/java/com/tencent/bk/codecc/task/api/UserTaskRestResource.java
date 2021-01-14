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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.TimeAnalysisConfigVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.devops.common.api.CommonPageVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.*;

import java.util.List;

/**
 * 任务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"USER_TASK"}, description = "任务管理接口")
@Path("/user/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskRestResource
{

    @ApiOperation("获取任务清单")
    @Path("/taskSortType/{taskSortType}")
    @POST
    CodeCCResult<TaskListVO> getTaskList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "任务排序类型", required = true)
            @PathParam(value = "taskSortType")
                    TaskSortType taskSortType,
            @ApiParam(value = "任务清单请求视图", required = true)
                    TaskListReqVO taskListReqVO
    );

    @ApiOperation("获取任务基本信息清单")
    @Path("/base")
    @GET
    CodeCCResult<TaskListVO> getTaskBaseList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String user);


    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    CodeCCResult<TaskBaseVO> getTaskInfo();


    @ApiOperation("获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    CodeCCResult<TaskDetailVO> getTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );

    @ApiOperation("获取任务信息概览")
    @Path("/overview/{taskId}")
    @GET
    CodeCCResult<TaskOverviewVO> getTaskOverview(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId);

    @ApiOperation("从持续集成平台注册新任务")
    @Path("/")
    @POST
    CodeCCResult<TaskIdVO> registerDevopsTask(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("修改任务扫描触发配置")
    @Path("/taskId/{taskId}/scanConfiguration")
    @POST
    CodeCCResult<Boolean> updateScanConfiguration(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "定时分析信息", required = true)
                    ScanConfigurationVO scanConfigurationVO
    );


    @ApiOperation("修改任务")
    @Path("/")
    @PUT
    CodeCCResult<Boolean> updateTask(
            //@Valid
            @ApiParam(value = "任务更新信息", required = true)
                    TaskUpdateVO taskUpdateVO,
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("添加路径屏蔽")
    @Path("/add/filter/path")
    @POST
    CodeCCResult<Boolean> addFilterPath(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    FilterPathInputVO filterPathInput,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("删除路径屏蔽")
    @Path("/del/filter")
    @DELETE
    CodeCCResult<Boolean> deleteFilterPath(
            @ApiParam(value = "删除路径", required = true)
            @QueryParam("path")
                    String path,
            @ApiParam(value = "路径类型", required = true)
            @QueryParam("pathType")
                    String pathType,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    CodeCCResult<FilterPathOutVO> filterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId
    );


    @ApiOperation("路径屏蔽树")
    @Path("/filter/path/tree")
    @GET
    CodeCCResult<TreeNodeTaskVO> filterPathTree(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("启用任务")
    @Path("/start")
    @PUT
    CodeCCResult<Boolean> startTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获得任务状态")
    @Path("/status")
    @GET
    CodeCCResult<TaskStatusVO> getTaskStatus(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);


    @ApiOperation("停用任务")
    @Path("/stop")
    @PUT
    CodeCCResult<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "停用原因", required = true)
                    String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获取代码库配置信息")
    @Path("/code/lib")
    @GET
    CodeCCResult<TaskCodeLibraryVO> getCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("更新代码库配置信息")
    @Path("/code/lib/update")
    @PUT
    CodeCCResult<Boolean> updateCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "代码库信息", required = true)
                    TaskDetailVO taskDetailVO
    ) throws JsonProcessingException;


    @ApiOperation("触发立即分析")
    @Path("/execute")
    @POST
    CodeCCResult<Boolean> executeTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "是否首次触发")
            @QueryParam("isFirstTrigger")
                    String isFirstTrigger,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);

    @ApiOperation("获取对任务有权限的各角色人员清单")
    @Path("/users")
    @GET
    CodeCCResult<TaskMemberVO> getTaskUsers(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("动态添加开源扫描任务")
    @Path("/openScan/startPage/{startPage}/endPage/{endPage}/startHour/{startHour}/startMinute/{startMinute}")
    @POST
    CodeCCResult<Boolean> extendGongfengScanRange(
            @ApiParam(value = "开始页面", required = true)
            @PathParam("startPage")
            Integer startPage,
            @ApiParam(value = "结束页面", required = true)
            @PathParam("endPage")
            Integer endPage,
            @ApiParam(value = "开始小时数", required = true)
            @PathParam("startHour")
            Integer startHour,
            @ApiParam(value = "开始分钟数", required = true)
            @PathParam("startMinute")
            Integer startMinute);

    @ApiOperation("保存定制化报告信息")
    @Path("/report")
    @POST
    CodeCCResult<Boolean> updateTaskReportInfo(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "通知信息", required = true)
            NotifyCustomVO notifyCustomVO);


    @ApiOperation("配置任务置顶")
    @Path("/top/config/taskId/{taskId}/topFlag/{topFlag}")
    @PUT
    CodeCCResult<Boolean> updateTopUserInfo(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(CODECC_AUTH_HEADER_DEVOPS_USER_ID)
            String user,
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "置顶标识", required = true)
            @PathParam("topFlag")
            Boolean topFlag);

    @ApiOperation("保存定制化报告信息")
    @Path("/dataSynchronization")
    @GET
    CodeCCResult<Boolean> syncKafkaTaskInfo(
            @ApiParam(value = "是否首次触发")
            @QueryParam("dataType")
                    String dataType,
            @ApiParam(value = "是否首次触发")
            @QueryParam("washTime")
                    String washTime
    );

    @ApiOperation("手动触发流水线")
    @Path("/manual/pipeline/trigger")
    @POST
    CodeCCResult<Boolean> manualTriggerPipeline(
            @ApiParam(value = "任务id清单")
            List<Long> taskIdList);

    @ApiOperation("更新任务成员和任务管理员")
    @Path("/member/taskId/{taskId}")
    @PUT
    CodeCCResult<Boolean> updateTaskOwnerAndMember(
            @ApiParam(value = "任务成员信息")
            TaskOwnerAndMemberVO taskOwnerAndMemberVO,
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            Long taskId);

    @ApiOperation("获取code.yml的路径屏蔽")
    @Path("/code/yml/filter/taskId/{taskId}/list")
    @GET
    CodeCCResult<CodeYmlFilterPathVO> listCodeYmlFilterPath(
        @ApiParam(value = "任务ID", required = true)
        @PathParam("taskId")
            Long taskId
    );

    @ApiOperation("触发蓝盾插件打分任务")
    @Path("/bkplugin/trigger")
    @GET
    CodeCCResult<Boolean> triggerBkPluginScoring();
}
