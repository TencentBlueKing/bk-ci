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
import com.tencent.bk.codecc.task.service.GongfengPublicProjService;
import com.tencent.bk.codecc.task.service.GongfengTriggerOldService;
import com.tencent.bk.codecc.task.service.GongfengTriggerService;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.CreateTaskConfigVO;
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
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.pojo.GongfengBaseInfo;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
    private PathFilterService pathFilterService;

    @Autowired
    private GongfengTriggerService gongfengTriggerService;

    @Autowired
    private GongfengTriggerOldService gongfengTriggerOldService;

    @Override
    public Result<TaskDetailVO> getTaskInfo(String nameEn) {
        return new Result<>(taskService.getTaskInfoByStreamName(nameEn));
    }

    @Override
    public Result<TaskBaseVO> getTaskToolList(long taskId) {
        return new Result<>(taskService.getTaskToolList(taskId));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoById(Long taskId)
    {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public Result<List<TaskBaseVO>> getTaskInfosByIds(List<Long> taskIds)
    {
        return new Result<>(taskService.getTasksByIds(taskIds));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoWithoutToolsByTaskId(Long taskId)
    {
        return new Result<>(taskService.getTaskInfoWithoutToolsByTaskId(taskId));
    }

    @Override
    public Result<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName)
    {
        log.info("upadte pipeline task request body: {}, username: {}", JsonUtil.INSTANCE.toJson(taskDetailVO), userName);
        return new Result<>(taskRegisterService.updateTask(taskDetailVO, userName));
    }

    @Override
    public Result<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName)
    {
        taskDetailVO.setProjectId(projectId);
        log.info("registerPipelineTask request body: {}", JsonUtil.INSTANCE.toJson(taskDetailVO));
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName)
    {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public Result<Boolean> stopTaskByPipeline(String pipelineId, String disabledReason, String userName) {
        return new Result<>(taskService.stopTask(pipelineId, disabledReason, userName));
    }

    @Override
    public Result<Boolean> checkTaskExists(Long taskId)
    {
        return new Result<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache()
    {
        return new Result<>(taskService.getToolMetaListFromCache());
    }

    @Override
    public Result<PipelineTaskVO> getPipelineTask(String pipelineId, String user)
    {
        return new Result<>(taskService.getTaskInfoByPipelineId(pipelineId, user));
    }

    @Override
    public Result<Set<String>> queryTaskListByPipelineIds(Set<String> pipelineIds) {
        return new Result<>(authTaskService.queryTaskListByPipelineIds(pipelineIds));
    }

    @Override
    public Result<TaskListVO> getTaskList(String projectId, String user)
    {
        return new Result<>(taskService.getTaskList(projectId, user, TaskSortType.CREATE_DATE, null));
    }

    @Override
    public Result<String> getGongfengRepoUrl(Long taskId)
    {
        return new Result<>(gongfengPublicProjService.getGongfengUrl(taskId));
    }

    @Override
    public Result<List<TaskBaseVO>> getTasksByBgId(Integer bgId)
    {
        return new Result<>(taskService.getTasksByBgId(bgId));
    }

    @Override
    public Result<TaskListVO> getTaskDetailList(QueryTaskListReqVO taskListReqVO)
    {
        return new Result<>(taskService.getTaskDetailList(taskListReqVO));
    }

    @Override
    public Result<Page<TaskInfoVO>> getTasksByAuthor(QueryMyTasksReqVO reqVO)
    {
        return new Result<>(taskService.getTasksByAuthor(reqVO));
    }

    @Override
    public Result<Boolean> updatePipelineTaskCheckerSets(String user, String projectId, String pipelineId, Long taskId,
                                                         UpdateCheckerSet2TaskReqVO updateCheckerSet2TaskReqVO)
    {
        return new Result<>(pipelineService.updateCheckerSets(user, projectId, pipelineId, taskId, updateCheckerSet2TaskReqVO.getToolCheckerSets()));
    }

    @Override
    public Result<Map<Integer, GongfengPublicProjVO>> getGongfengProjInfo(Collection<Integer> gfProjectId)
    {
        return new Result<>(gongfengPublicProjService.queryGongfengProjectMapById(gfProjectId));
    }

    @Override
    public Result<Boolean> syncGongfengStatProj(Integer bgId)
    {
        return new Result<>(gongfengPublicProjService.saveStatProject(bgId));
    }

    @Override
    public Result<Map<Integer, ProjectStatVO>> getGongfengStatProjInfo(Integer bgId, Collection<Integer> gfProjectId)
    {
        return new Result<>(gongfengPublicProjService.queryGongfengStatProjectById(bgId, gfProjectId));
    }

    @Override
    public Result<GongfengBaseInfo> getGongfengBaseInfo(Long taskId)
    {
        return new Result<>(authTaskService.getGongfengProjInfo(taskId));
    }

    @Override
    public Result<Set<Integer>> queryDeptIdByBgId(Integer bgId)
    {
        Set<Integer> deptIdSet = taskService.queryDeptIdByBgId(bgId);
        return new Result<>(deptIdSet);
    }

    @Override
    public Result<List<TaskDetailVO>> batchGetTaskList(QueryTaskListReqVO queryTaskListReqVO)
    {
        return new Result<>(taskService.getTaskInfoList(queryTaskListReqVO));
    }


    @Override
    public Result<FilterPathOutVO> filterPath(Long taskId) {
        return new Result<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public Result<TriggerPipelineOldRsp> triggerCustomPipeline(TriggerPipelineOldReq triggerPipelineReq, String userId)
    {
        return new Result<>(gongfengTriggerOldService.triggerCustomProjectPipeline(triggerPipelineReq, userId));
    }

    @Override
    public Result<TriggerPipelineRsp> triggerCustomPipelineNew(TriggerPipelineReq triggerPipelineReq, String appCode, String userId)
    {
        return new Result<>(gongfengTriggerService.triggerCustomProjectPipeline(triggerPipelineReq, appCode, userId));
    }

    @Override
    public Result<Page<CustomProjVO>> batchGetCustomTaskList(QueryTaskListReqVO reqVO)
    {
        return new Result<>(gongfengPublicProjService.queryCustomTaskByPageable(reqVO));
    }

    @Override
    public Result<Page<TaskDetailVO>> getTaskDetailPage(QueryTaskListReqVO reqVO)
    {
        return new Result<>(taskService.getTaskDetailPage(reqVO));
    }

    @Override
    public Result<Boolean> authorTransfer(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs, String userId)
    {
        taskService.authorTransferForApi(taskId, transferAuthorPairs, userId);
        return new Result<>(true);
    }

    @Override
    public Result<List<Long>> getBkPluginTaskIds() {
        return new Result<>(taskService.getBkPluginTaskIds());
    }

    @Override
    public Result<Map<Long, GongfengPublicProjVO>> getGongfengProjInfoByTaskId(List<Long> taskId) {
        if (CollectionUtils.isEmpty(taskId)) {
            return new Result<>(null);
        }
        return new Result<>(gongfengPublicProjService.queryGongfengProjectMapByTaskId(taskId));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoByAliasName(String aliasName) {
        if (StringUtils.isBlank(aliasName)) {
            throw new CodeCCException(com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_,
                    new String[]{"无效的代码库别名"});
        }

        return new Result<>(taskService.getTaskInfoByAliasName(aliasName));
    }

    @Override public Result<TaskDetailVO> getTaskInfoByGongfengId(Integer id) {
        return new Result<>(taskService.getTaskInfoByGongfengId(id, null));
    }

    @Override
    public Result<GongfengBaseInfo> getGongfengCIBaseInfo(Integer gongfengId) {
        return new Result<>(authTaskService.getGongfengCIProjInfo(gongfengId));
    }

    @Override public Result<Boolean> createTaskForBkPlugins(String repoId, CreateTaskConfigVO createTaskConfigVO) {
        if (StringUtils.isBlank(repoId) || createTaskConfigVO.getLangs().isEmpty()) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL);
        }

        try {
            boolean isSucc = gongfengTriggerService.createTaskByRepoId(repoId, createTaskConfigVO.getLangs());
            if (isSucc) {
                return new Result<>(true);
            } else {
                return new Result<>(2300021,
                        CommonMessageCode.SYSTEM_ERROR,
                        "工蜂任务不合法",
                        false);
            }
        } catch (CodeCCException e) {
            return new Result<>(2300021, "2300021", "任务创建失败", false);
        }
    }

    @Override
    public Result<List<Long>> queryTaskIdByCreateFrom(List<String> createFrom) {
        return new Result<>(taskService.queryTaskIdByCreateFrom(createFrom));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoWithoutToolsByStreamName(String nameEn) {
        return new Result<>(taskService.getTaskInfoWithoutToolsByStreamName(nameEn));
    }

    @Override
    public Result<Boolean> stopRunningApiTask(String codeccBuildId, String appCode, String userId) {
        try {
            gongfengTriggerService.stopRunningApiTask(codeccBuildId, appCode, userId);
        } catch (Exception e) {
            log.info("stop running api task:{}", e.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, e.getMessage());
        }
        return new Result<>(true);
    }

}
