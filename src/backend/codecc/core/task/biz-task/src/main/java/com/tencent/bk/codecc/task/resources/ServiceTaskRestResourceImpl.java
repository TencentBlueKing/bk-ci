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

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp;
import com.tencent.bk.codecc.task.service.*;
import com.tencent.bk.codecc.task.vo.*;
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
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.pojo.GongfengBaseInfo;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服务间任务管理接口
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Slf4j
@RestResource
public class ServiceTaskRestResourceImpl implements ServiceTaskRestResource
{
    @Autowired
    private TaskService taskService;

    @Autowired
    private GongfengPublicProjService gongfengPublicProjService;

    @Autowired
    @Qualifier("pipelineTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private KafkaSyncService kafkaSyncService;

    @Autowired
    private PathFilterService pathFilterService;

    @Autowired
    private GongfengTriggerService gongfengTriggerService;

    @Autowired
    private GongfengTriggerOldService gongfengTriggerOldService;

    @Override
    public CodeCCResult<TaskDetailVO> getTaskInfo(String nameEn)
    {
        return new CodeCCResult<>(taskService.getTaskInfoByStreamName(nameEn));
    }

    @Override
    public CodeCCResult<TaskBaseVO> getTaskToolList(long taskId)
    {
        return new CodeCCResult<>(taskService.getTaskToolList(taskId));
    }

    @Override
    public CodeCCResult<TaskDetailVO> getTaskInfoById(Long taskId)
    {
        return new CodeCCResult<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public CodeCCResult<List<TaskBaseVO>> getTaskInfosByIds(List<Long> taskIds)
    {
        return new CodeCCResult<>(taskService.getTasksByIds(taskIds));
    }

    @Override
    public CodeCCResult<TaskDetailVO> getTaskInfoWithoutToolsByTaskId(Long taskId)
    {
        return new CodeCCResult<>(taskService.getTaskInfoWithoutToolsByTaskId(taskId));
    }

    @Override
    public CodeCCResult<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName)
    {
        log.info("upadte pipeline task request body: {}, username: {}", JsonUtil.INSTANCE.toJson(taskDetailVO), userName);
        return new CodeCCResult<>(taskRegisterService.updateTask(taskDetailVO, userName));
    }

    @Override
    public CodeCCResult<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName)
    {
        taskDetailVO.setProjectId(projectId);
        log.info("registerPipelineTask request body: {}", JsonUtil.INSTANCE.toJson(taskDetailVO));
        return new CodeCCResult<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public CodeCCResult<Boolean> stopTask(Long taskId, String disabledReason, String userName)
    {
        return new CodeCCResult<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public CodeCCResult<Boolean> stopTaskByPipeline(String pipelineId, String disabledReason, String userName) {
        return new CodeCCResult<>(taskService.stopTask(pipelineId, disabledReason, userName));
    }

    @Override
    public CodeCCResult<Boolean> checkTaskExists(Long taskId)
    {
        return new CodeCCResult<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public CodeCCResult<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache()
    {
        return new CodeCCResult<>(taskService.getToolMetaListFromCache());
    }

    @Override
    public CodeCCResult<PipelineTaskVO> getPipelineTask(String pipelineId, String user)
    {
        return new CodeCCResult<>(taskService.getTaskInfoByPipelineId(pipelineId, user));
    }

    @Override
    public CodeCCResult<TaskListVO> getTaskList(String projectId, String user)
    {
        return new CodeCCResult<>(taskService.getTaskList(projectId, user, TaskSortType.CREATE_DATE, null));
    }

    @Override
    public CodeCCResult<String> getGongfengRepoUrl(Long taskId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.getGongfengUrl(taskId));
    }

    @Override
    public CodeCCResult<List<TaskBaseVO>> getTasksByBgId(Integer bgId)
    {
        return new CodeCCResult<>(taskService.getTasksByBgId(bgId));
    }

    @Override
    public CodeCCResult<TaskListVO> getTaskDetailList(QueryTaskListReqVO taskListReqVO)
    {
        return new CodeCCResult<>(taskService.getTaskDetailList(taskListReqVO));
    }

    @Override
    public CodeCCResult<Page<TaskInfoVO>> getTasksByAuthor(QueryMyTasksReqVO reqVO)
    {
        return new CodeCCResult<>(taskService.getTasksByAuthor(reqVO));
    }

    @Override
    public CodeCCResult<Boolean> updatePipelineTaskCheckerSets(String user, String projectId, String pipelineId, Long taskId,
                                                               UpdateCheckerSet2TaskReqVO updateCheckerSet2TaskReqVO)
    {
        return new CodeCCResult<>(pipelineService.updateCheckerSets(user, projectId, pipelineId, taskId, updateCheckerSet2TaskReqVO.getToolCheckerSets()));
    }

    @Override
    public CodeCCResult<Map<Integer, GongfengPublicProjVO>> getGongfengProjInfo(Collection<Integer> gfProjectId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryGongfengProjectMapById(gfProjectId));
    }

    @Override
    public CodeCCResult<Boolean> syncGongfengStatProj(Integer bgId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.saveStatProject(bgId));
    }

    @Override
    public CodeCCResult<Map<Integer, ProjectStatVO>> getGongfengStatProjInfo(Integer bgId, Collection<Integer> gfProjectId)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryGongfengStatProjectById(bgId, gfProjectId));
    }

    @Override
    public CodeCCResult<GongfengBaseInfo> getGongfengBaseInfo(Long taskId)
    {
        return new CodeCCResult<>(authTaskService.getGongfengProjInfo(taskId));
    }

    @Override
    public CodeCCResult<Page<Long>> getTaskInfoByCreateFrom(String taskType, CommonPageVO reqVO)
    {
        Page<Long> list = kafkaSyncService.getTaskInfoByCreateFrom(taskType, reqVO);
        return new CodeCCResult<>(list);
    }

    @Override
    public CodeCCResult<Set<Integer>> queryDeptIdByBgId(Integer bgId)
    {
        Set<Integer> deptIdSet = taskService.queryDeptIdByBgId(bgId);
        return new CodeCCResult<>(deptIdSet);
    }

    @Override
    public CodeCCResult<List<TaskDetailVO>> batchGetTaskList(QueryTaskListReqVO queryTaskListReqVO)
    {
        return new CodeCCResult<>(taskService.getTaskInfoList(queryTaskListReqVO));
    }


    @Override
    public CodeCCResult<FilterPathOutVO> filterPath(Long taskId) {
        return new CodeCCResult<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public CodeCCResult<TriggerPipelineOldRsp> triggerCustomPipeline(TriggerPipelineOldReq triggerPipelineReq, String userId)
    {
        return new CodeCCResult<>(gongfengTriggerOldService.triggerCustomProjectPipeline(triggerPipelineReq, userId));
    }

    @Override
    public CodeCCResult<TriggerPipelineRsp> triggerCustomPipelineNew(TriggerPipelineReq triggerPipelineReq, String appCode, String userId)
    {
        return new CodeCCResult<>(gongfengTriggerService.triggerCustomProjectPipeline(triggerPipelineReq, appCode, userId));
    }

    @Override
    public CodeCCResult<Page<CustomProjVO>> batchGetCustomTaskList(QueryTaskListReqVO reqVO)
    {
        return new CodeCCResult<>(gongfengPublicProjService.queryCustomTaskByPageable(reqVO));
    }

    @Override
    public CodeCCResult<Page<TaskDetailVO>> getTaskDetailPage(QueryTaskListReqVO reqVO)
    {
        return new CodeCCResult<>(taskService.getTaskDetailPage(reqVO));
    }

    @Override
    public CodeCCResult<Boolean> authorTransfer(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs, String userId)
    {
        taskService.authorTransferForApi(taskId, transferAuthorPairs, userId);
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<List<Long>> getBkPluginTaskIds() {
        return new CodeCCResult<>(taskService.getBkPluginTaskIds());
    }
}
