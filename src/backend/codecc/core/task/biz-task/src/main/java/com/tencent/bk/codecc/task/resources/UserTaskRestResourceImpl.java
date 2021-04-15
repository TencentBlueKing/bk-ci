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

package com.tencent.bk.codecc.task.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.api.UserTaskRestResource;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.service.*;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.CommonPageVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

/**
 * 任务清单资源实现
 *
 * @version V1.0
 * @date 2019/4/23
 */
@RestResource
public class UserTaskRestResourceImpl implements UserTaskRestResource {
    @Autowired
    private TaskService taskService;

    @Autowired
    private GongfengPublicProjService gongfengPublicProjService;

    @Autowired
    @Qualifier("devopsTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PathFilterService pathFilterService;

    @Autowired
    private KafkaSyncService kafkaSyncService;


    @Override
    public CodeCCResult<TaskListVO> getTaskList(String projectId, String user, TaskSortType taskSortType, TaskListReqVO taskListReqVO) {
        return new CodeCCResult<>(taskService.getTaskList(projectId, user, taskSortType, taskListReqVO));
    }

    @Override
    public CodeCCResult<TaskListVO> getTaskBaseList(String projectId, String user) {
        return new CodeCCResult<>(taskService.getTaskBaseList(projectId, user));
    }

    @Override
    public CodeCCResult<TaskBaseVO> getTaskInfo() {
        return new CodeCCResult<>(taskService.getTaskInfo());
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.REPORT_VIEW})
    public CodeCCResult<TaskDetailVO> getTask(Long taskId) {
        return new CodeCCResult<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public CodeCCResult<TaskOverviewVO> getTaskOverview(Long taskId) {
        return new CodeCCResult<>(taskService.getTaskOverview(taskId));
    }


    @Override
    public CodeCCResult<TaskIdVO> registerDevopsTask(TaskDetailVO taskDetailVO, String projectId, String userName) {
        taskDetailVO.setProjectId(projectId);
        return new CodeCCResult<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
//    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO) {
        return new CodeCCResult<>(taskService.updateScanConfiguration(taskId, user, scanConfigurationVO));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String projectId, String userName) {
        return new CodeCCResult<>(taskService.updateTask(taskUpdateVO, taskId, userName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> addFilterPath(FilterPathInputVO filterPathInput, String userName) {
        return new CodeCCResult<>(pathFilterService.addFilterPaths(filterPathInput, userName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> deleteFilterPath(String path, String pathType, Long taskId, String userName) {
        return new CodeCCResult<>(pathFilterService.deleteFilterPath(path, pathType, taskId, userName));
    }

    @Override
    public CodeCCResult<FilterPathOutVO> filterPath(Long taskId) {
        return new CodeCCResult<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public CodeCCResult<TreeNodeTaskVO> filterPathTree(Long taskId) {
        return new CodeCCResult<>(pathFilterService.filterPathTree(taskId));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> startTask(Long taskId, String userName) {
        return new CodeCCResult<>(taskService.startTask(taskId, userName));
    }

    @Override
    public CodeCCResult<TaskStatusVO> getTaskStatus(Long taskId) {
        return new CodeCCResult<>(taskService.getTaskStatus(taskId));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> stopTask(Long taskId, String disabledReason, String userName) {
        return new CodeCCResult<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public CodeCCResult<TaskCodeLibraryVO> getCodeLibrary(Long taskId) {
        return new CodeCCResult<>(taskService.getCodeLibrary(taskId));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) throws JsonProcessingException {
        return new CodeCCResult<>(taskService.updateCodeLibrary(taskId, userName, taskDetailVO));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.ANALYZE})
    public CodeCCResult<Boolean> executeTask(long taskId, String isFirstTrigger,
                                       String userName) {
        return new CodeCCResult<>(taskService.manualExecuteTask(taskId, isFirstTrigger, userName));
    }

    @Override
    public CodeCCResult<TaskMemberVO> getTaskUsers(long taskId, String projectId) {
        return new CodeCCResult<>(taskService.getTaskUsers(taskId, projectId));
    }

    @Override
    public CodeCCResult<Boolean> extendGongfengScanRange(Integer startPage, Integer endPage, Integer startHour, Integer startMinute) {
        return new CodeCCResult<>(gongfengPublicProjService.extendGongfengScanRange(startPage, endPage, startHour, startMinute));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> updateTaskReportInfo(Long taskId, NotifyCustomVO notifyCustomVO) {
        taskService.updateReportInfo(taskId, notifyCustomVO);
        return new CodeCCResult<>(true);
    }


    @Override
    public CodeCCResult<Boolean> updateTopUserInfo(String user, Long taskId, Boolean topFlag)
    {
        return new CodeCCResult<>(taskService.updateTopUserInfo(taskId, user, topFlag));
    }

    @Override
    public CodeCCResult<Boolean> syncKafkaTaskInfo(String dataType, String washTime) {
        return new CodeCCResult<>(kafkaSyncService.syncTaskInfoToKafkaByType(dataType, washTime));
    }

    @Override
    public CodeCCResult<Boolean> manualTriggerPipeline(List<Long> taskIdList) {
        kafkaSyncService.manualExecuteTriggerPipeline(taskIdList);
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<Boolean> updateTaskOwnerAndMember(TaskOwnerAndMemberVO taskOwnerAndMemberVO, Long taskId)
    {
        taskService.updateTaskOwnerAndMember(taskOwnerAndMemberVO, taskId);
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<CodeYmlFilterPathVO> listCodeYmlFilterPath(Long taskId) {
        return new CodeCCResult<>(pathFilterService.listCodeYmlFilterPath(taskId));
    }

    @Override
    public CodeCCResult<Boolean> triggerBkPluginScoring() {
        return new CodeCCResult<>(taskService.triggerBkPluginScoring());
    }
}
