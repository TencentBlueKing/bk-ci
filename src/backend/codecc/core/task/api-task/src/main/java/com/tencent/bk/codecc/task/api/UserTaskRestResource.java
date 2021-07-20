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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.vo.CreateTaskConfigVO;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
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
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

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
    @ApiOperation("触发通知")
    @Path("/triggerNotify")
    @GET
    Result<Boolean> triggerNotify(
            @QueryParam(value = "taskId")
            Long taskId,
            @QueryParam(value = "type")
            Integer type);

    @ApiOperation("获取任务清单")
    @Path("/taskSortType/{taskSortType}")
    @POST
    Result<TaskListVO> getTaskList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
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
    Result<TaskListVO> getTaskBaseList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user);


    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    Result<TaskBaseVO> getTaskInfo();


    @ApiOperation("获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );

    @ApiOperation("获取任务信息概览")
    @Path("/overview/{taskId}")
    @GET
    Result<TaskOverviewVO> getTaskOverview(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "构建号", required = true)
            @QueryParam(value = "buildNum")
                String buildNum,
            @ApiParam(value = "依据")
            @QueryParam(value = "orderBy")
                    String orderBy);

    @ApiOperation("从持续集成平台注册新任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerDevopsTask(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("修改任务扫描触发配置")
    @Path("/taskId/{taskId}/scanConfiguration")
    @POST
    Result<Boolean> updateScanConfiguration(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "定时分析信息", required = true)
                    ScanConfigurationVO scanConfigurationVO
    );


    @ApiOperation("修改任务")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            //@Valid
            @ApiParam(value = "任务更新信息", required = true)
                    TaskUpdateVO taskUpdateVO,
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("添加路径屏蔽")
    @Path("/add/filter/path")
    @POST
    Result<Boolean> addFilterPath(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    FilterPathInputVO filterPathInput,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("删除路径屏蔽")
    @Path("/del/filter")
    @DELETE
    Result<Boolean> deleteFilterPath(
            @ApiParam(value = "删除路径", required = true)
            @QueryParam("path")
                    String path,
            @ApiParam(value = "路径类型", required = true)
            @QueryParam("pathType")
                    String pathType,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId
    );


    @ApiOperation("路径屏蔽树")
    @Path("/filter/path/tree")
    @GET
    Result<TreeNodeTaskVO> filterPathTree(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("启用任务")
    @Path("/start")
    @PUT
    Result<Boolean> startTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获得任务状态")
    @Path("/status")
    @GET
    Result<TaskStatusVO> getTaskStatus(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);


    @ApiOperation("停用任务")
    @Path("/stop")
    @PUT
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "停用原因", required = true)
                    String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获取代码库配置信息")
    @Path("/code/lib")
    @GET
    Result<TaskCodeLibraryVO> getCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("更新代码库配置信息")
    @Path("/code/lib/update")
    @PUT
    Result<Boolean> updateCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "代码库信息", required = true)
                    TaskDetailVO taskDetailVO
    ) throws JsonProcessingException;


    @ApiOperation("触发立即分析")
    @Path("/execute")
    @POST
    Result<Boolean> executeTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "是否首次触发")
            @QueryParam("isFirstTrigger")
                    String isFirstTrigger,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);

    @ApiOperation("获取对任务有权限的各角色人员清单")
    @Path("/users")
    @GET
    Result<TaskMemberVO> getTaskUsers(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );

    @ApiOperation("动态添加开源扫描任务")
    @Path("/openScan/startPage/{startPage}/endPage/{endPage}/startHour/{startHour}/startMinute/{startMinute}")
    @POST
    Result<Boolean> extendGongfengScanRange(
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
    Result<Boolean> updateTaskReportInfo(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "通知信息", required = true)
            NotifyCustomVO notifyCustomVO);


    @ApiOperation("配置任务置顶")
    @Path("/top/config/taskId/{taskId}/topFlag/{topFlag}")
    @PUT
    Result<Boolean> updateTopUserInfo(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "置顶标识", required = true)
            @PathParam("topFlag")
            Boolean topFlag);

    @ApiOperation("更新任务成员和任务管理员")
    @Path("/member/taskId/{taskId}")
    @PUT
    Result<Boolean> updateTaskOwnerAndMember(
            @ApiParam(value = "任务成员信息")
            TaskOwnerAndMemberVO taskOwnerAndMemberVO,
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
            Long taskId);

    @ApiOperation("获取code.yml的路径屏蔽")
    @Path("/code/yml/filter/taskId/{taskId}/list")
    @GET
    Result<CodeYmlFilterPathVO> listCodeYmlFilterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId
    );

    @ApiOperation("触发蓝盾插件打分任务")
    @Path("/bkplugin/trigger")
    @GET
    Result<Boolean> triggerBkPluginScoring();

    @ApiOperation("查询工具维度信息")
    @Path("/listDimension")
    @GET
    Result<List<MetadataVO>> listTaskToolDimension(
        @ApiParam(value = "任务ID", required = true)
        @QueryParam("taskId")
            Long taskId
    );

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
}
