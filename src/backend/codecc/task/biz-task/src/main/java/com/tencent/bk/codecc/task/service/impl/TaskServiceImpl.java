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

package com.tencent.bk.codecc.task.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.ToolMetaCache;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.DisableTaskEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.devops.common.api.GetLastAnalysisResultsVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.BkAuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi;
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.pipeline.pojo.element.Element;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.constant.ComConstants.*;

/**
 * 任务服务实现类
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Service
public class TaskServiceImpl implements TaskService
{
    private static Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private BkAuthExPermissionApi bkAuthExPermissionApi;

    @Autowired
    private BkAuthExRegisterApi bkAuthExRegisterApi;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolMetaCache toolMetaCache;

    @Autowired
    private CommonDao commonDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToolDao toolDao;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;


    @Override
    public TaskListVO getTaskList(String projectId, String user)
    {
        Set<TaskInfoEntity> resultTasks = getQualifiedTaskList(projectId, user);

        final String toolIdsOrder = commonDao.getToolOrder();

        List<TaskDetailVO> taskDetailVOList = resultTasks.stream().
                filter(taskInfoEntity ->
                        taskInfoEntity.getTaskId() != 0L &&
                                StringUtils.isNotEmpty(taskInfoEntity.getProjectId()) &&
                                projectId.equals(taskInfoEntity.getProjectId()) &&
                                StringUtils.isNotEmpty(taskInfoEntity.getToolNames())).
                map(taskInfoEntity ->
                {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
                    taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
                    return taskDetailVO;
                }).
                collect(Collectors.toList());

        Result<Map<String, List<ToolLastAnalysisResultVO>>> taskAndTaskLogResult = client.get(ServiceTaskLogRestResource.class).
                getBatchTaskLatestTaskLog(taskDetailVOList);
        Map<String, List<ToolLastAnalysisResultVO>> taskAndTaskLogMap;
        if (taskAndTaskLogResult.isOk() &&
                MapUtils.isNotEmpty(taskAndTaskLogResult.getData()))
        {
            taskAndTaskLogMap = taskAndTaskLogResult.getData();
        }
        else
        {
            logger.error("get batch task log fail or task log is empty!");
            taskAndTaskLogMap = new HashMap<>();
        }


        //对工具清单进行处理
        List<TaskDetailVO> taskDetailVOS = resultTasks.stream().
                filter(taskInfoEntity ->
                        taskInfoEntity.getTaskId() != 0L &&
                                StringUtils.isNotEmpty(taskInfoEntity.getProjectId()) &&
                                projectId.equals(taskInfoEntity.getProjectId())).
                map(taskInfoEntity ->
                {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    BeanUtils.copyProperties(taskInfoEntity, taskDetailVO, "toolConfigInfoList");
                    List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
                    //获取分析完成时间
                    List<ToolLastAnalysisResultVO> taskLogGroupVOs = new ArrayList<>();
                    String toolNames = taskInfoEntity.getToolNames();
                    if (StringUtils.isNotEmpty(toolNames))
                    {
                        if (MapUtils.isNotEmpty(taskAndTaskLogMap))
                        {
                            taskLogGroupVOs = taskAndTaskLogMap.get(String.valueOf(taskInfoEntity.getTaskId()));
                            if (null == taskLogGroupVOs)
                            {
                                taskLogGroupVOs = new ArrayList<>();
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList))
                    {
                        List<ToolConfigInfoVO> toolConfigInfoVOList = new ArrayList<>();
                        boolean isAllSuspended = true;
                        for (ToolConfigInfoEntity toolConfigInfoEntity : toolConfigInfoEntityList)
                        {
                            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO);
                            //设置分析完成时间
                            for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : taskLogGroupVOs)
                            {
                                if (toolLastAnalysisResultVO.getToolName().equalsIgnoreCase(toolConfigInfoVO.getToolName()))
                                {
                                    toolConfigInfoVO.setEndTime(toolLastAnalysisResultVO.getEndTime());
                                }
                            }
                            ToolMetaEntity toolMetaEntity = toolMetaCache.getToolFromCache(toolConfigInfoVO.getToolName());
                            if (null != toolMetaEntity)
                            {
                                String displayName = toolMetaEntity.getDisplayName();
                                toolConfigInfoVO.setDisplayName(displayName);
                            }
                            if (toolConfigInfoVO.getFollowStatus() !=
                                    ComConstants.FOLLOW_STATUS.WITHDRAW.value())
                            {
                                isAllSuspended = false;
                            }
                            toolConfigInfoVOList.add(toolConfigInfoVO);
                        }
                        if (isAllSuspended)
                        {
                            logger.info("all tool is suspended! task id: {}", taskInfoEntity.getTaskId());
                            toolConfigInfoVOList.get(0).setFollowStatus(ComConstants.FOLLOW_STATUS.EXPERIENCE.value());
                        }

                        logger.info("handle tool list finish! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(toolConfigInfoVOList);
                    }
                    else
                    {
                        logger.info("tool list is empty! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(new ArrayList<>());
                    }

                    List<ToolConfigInfoVO> toolConfigInfoVOs = new ArrayList<>();
                    //重置工具顺序，并且对工具清单顺序也进行重排
                    taskDetailVO.setToolNames(resetToolOrderByType(taskDetailVO.getToolNames(), toolIdsOrder, taskDetailVO.getToolConfigInfoList(),
                            toolConfigInfoVOs));
                    taskDetailVO.setToolConfigInfoList(toolConfigInfoVOs);
                    return taskDetailVO;
                }).
                collect(Collectors.toList());
        return sortByDate(taskDetailVOS);
    }


    @Override
    public TaskListVO getTaskBaseList(String projectId, String user)
    {
        Set<TaskInfoEntity> resultSet = getQualifiedTaskList(projectId, user);
        if (CollectionUtils.isNotEmpty(resultSet))
        {
            List<TaskDetailVO> taskBaseVOList = resultSet.stream().map(taskInfoEntity ->
            {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
                taskDetailVO.setEntityId(taskInfoEntity.getEntityId());
                taskDetailVO.setNameCn(taskInfoEntity.getNameCn());
                taskDetailVO.setNameEn(taskInfoEntity.getNameEn());
                taskDetailVO.setStatus(taskInfoEntity.getStatus());
                taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
                taskDetailVO.setCreatedDate(taskInfoEntity.getCreatedDate());
                taskDetailVO.setDisableTime(taskInfoEntity.getDisableTime());
                return taskDetailVO;
            }).
                    collect(Collectors.toList());
            List<TaskDetailVO> enableTaskList = taskBaseVOList.stream()
                    .filter(taskDetailVO ->
                            TaskConstants.TaskStatus.DISABLE.value() !=
                                    taskDetailVO.getStatus())
                    .sorted((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()))
                    .collect(Collectors.toList());
            List<TaskDetailVO> disableTaskList = taskBaseVOList.stream()
                    .filter(taskDetailVO ->
                            TaskConstants.TaskStatus.DISABLE.value() ==
                                    taskDetailVO.getStatus())
                    .sorted((o1, o2) -> (StringUtils.isEmpty(o2.getDisableTime()) ? Long.valueOf(0) : Long.valueOf(o2.getDisableTime()))
                            .compareTo(StringUtils.isEmpty(o1.getDisableTime()) ? Long.valueOf(0) : Long.valueOf(o1.getDisableTime())))
                    .collect(Collectors.toList());
            return new TaskListVO(enableTaskList, disableTaskList);


        }
        return null;
    }


    /**
     * 查询符合条件的任务清单
     *
     * @param projectId
     * @param user
     * @return
     */
    private Set<TaskInfoEntity> getQualifiedTaskList(String projectId, String user)
    {
        //先到权限中心查询资源清单
        Set<Long> taskList = bkAuthExPermissionApi.queryResourceListForUser(user, projectId,
                Arrays.asList(BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER));

        //添加demo任务
        Set<TaskInfoEntity> taskDetails = taskRepository.findByTaskIdIn(taskList);
        //加上从流水线删除的codecc原子关联的项目，此部分项目已不再蓝盾的资源范围内
        Set<TaskInfoEntity> allUserTasks = taskRepository.findTaskList(user, projectId, TaskConstants.TaskStatus.DISABLE.value());
        Set<TaskInfoEntity> resultTasks = new HashSet<>();
        if (CollectionUtils.isNotEmpty(taskDetails))
        {
            resultTasks.addAll(taskDetails);
        }
        if (CollectionUtils.isNotEmpty(allUserTasks))
        {
            resultTasks.addAll(allUserTasks);
        }
        logger.info("task mongorepository finish, list length: {}", resultTasks.size());
        return resultTasks;
    }


    @Override
    public TaskBaseVO getTaskInfo()
    {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID);
        logger.info("getTaskInfo: {}", taskId);
        if (!StringUtils.isNumeric(taskId))
        {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{String.valueOf(taskId)}, null);
        }
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(Long.valueOf(taskId));

        if (taskEntity == null)
        {
            logger.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{taskId}, null);
        }

        TaskBaseVO taskBaseVO = new TaskBaseVO();
        BeanUtils.copyProperties(taskEntity, taskBaseVO);
        List<ToolConfigInfoEntity> toolEntityList = taskEntity.getToolConfigInfoList();

        // 给工具分类及排序
        sortedToolList(taskBaseVO, toolEntityList);

        return taskBaseVO;
    }

    /**
     * 获取任务接入的工具列表
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskBaseVO getTaskToolList(long taskId)
    {
        List<ToolConfigInfoEntity> toolEntityList = toolRepository.findByTaskId(Long.valueOf(taskId));

        TaskBaseVO taskBaseVO = new TaskBaseVO();

        // 给工具分类及排序
        sortedToolList(taskBaseVO, toolEntityList);

        return taskBaseVO;
    }

    @Override
    public TaskBaseVO getTaskInfo(String nameEn)
    {
        TaskInfoEntity taskEntity = taskRepository.findByNameEn(nameEn);

        if (taskEntity == null)
        {
            logger.error("can not find task by nameEn: {}", nameEn);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{nameEn}, null);
        }

        TaskBaseVO taskBaseVO = new TaskBaseVO();
        BeanUtils.copyProperties(taskEntity, taskBaseVO);
        return taskBaseVO;
    }


    /**
     * 修改任务
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_INFO, operType = MODIFY_INFO)
    public Boolean updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String userName)
    {
        // 检查参数
        if (!checkParam(taskUpdateVO))
        {
            return false;
        }

        // 任务是否注册过
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskInfoEntity))
        {
            logger.error("can not find task info");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 修改任务信息
        taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(), taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);

        // 判断是否存在流水线，如果是则将任务语言同步到蓝盾流水线编排
        if (StringUtils.isNotBlank(taskInfoEntity.getPipelineId()))
        {
            if (taskInfoEntity.getCreateFrom().equals(ComConstants.BsTaskCreateFrom.BS_CODECC.value()))
            {
                // codecc服务创建的任务
                pipelineService.updateBsPipelineLang(taskInfoEntity, userName);
            }
            else if (taskInfoEntity.getCreateFrom().equals(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()))
            {
                // 流水线添加codecc原子创建的任务, 存在语言
                pipelineService.updateBsPipelineLangBSChannelCode(taskInfoEntity, userName);
            }
        }

        return true;
    }

    /**
     * 修改任务基本信息 - 内部服务间调用
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    @Override
    public Boolean updateTaskByServer(TaskUpdateVO taskUpdateVO, String userName)
    {
        return taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(), taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);
    }

    @Override
    public TaskDetailVO getTaskInfoById(Long taskId)
    {
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (taskEntity == null)
        {
            logger.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        try
        {
            String taskInfoStr = objectMapper.writeValueAsString(taskEntity);
            return objectMapper.readValue(taskInfoStr, new TypeReference<TaskDetailVO>()
            {
            });
        }
        catch (IOException e)
        {
            String message = "string conversion TaskDetailVO error";
            logger.error(message);
            throw new StreamException(message);
        }
    }

    @Override
    public TaskOverviewVO getTaskOverview(Long taskId)
    {
        TaskInfoEntity taskEntity = taskRepository.findToolListByTaskId(taskId);
        if (taskEntity == null)
        {
            logger.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        TaskOverviewVO taskOverviewVO = new TaskOverviewVO();
        taskOverviewVO.setTaskId(taskId);
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        if (CollectionUtils.isNotEmpty(toolConfigInfoList))
        {
            List<TaskOverviewVO.LastAnalysis> toolLastAnalysisList = new ArrayList<>();
            Map<String, TaskOverviewVO.LastAnalysis> toolLastAnalysisMap = new HashMap<>();
            for (ToolConfigInfoEntity tool : toolConfigInfoList)
            {
                int followStatus = tool.getFollowStatus();
                if (followStatus != TaskConstants.FOLLOW_STATUS.WITHDRAW.value())
                {
                    TaskOverviewVO.LastAnalysis lastAnalysis = new TaskOverviewVO.LastAnalysis();
                    String toolName = tool.getToolName();
                    lastAnalysis.setToolName(toolName);
                    lastAnalysis.setCurStep(tool.getCurStep());
                    lastAnalysis.setStepStatus(tool.getStepStatus());
                    toolLastAnalysisMap.put(toolName, lastAnalysis);
                    toolLastAnalysisList.add(lastAnalysis);
                }
            }

            // 调用defect模块的接口获取工具的最近一次分析结果
            GetLastAnalysisResultsVO getLastAnalysisResultsVO = new GetLastAnalysisResultsVO();
            getLastAnalysisResultsVO.setTaskId(taskId);
            getLastAnalysisResultsVO.setToolSet(toolLastAnalysisMap.keySet());
            Result<List<ToolLastAnalysisResultVO>> result = client.get(ServiceTaskLogRestResource.class).getLastAnalysisResults(getLastAnalysisResultsVO);
            if (result.isNotOk() || null == result.getData())
            {
                logger.error("get last analysis results fail! taskId is: {}, msg: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            List<ToolLastAnalysisResultVO> lastAnalysisResultVOs = result.getData();

            if (CollectionUtils.isNotEmpty(lastAnalysisResultVOs))
            {
                for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : lastAnalysisResultVOs)
                {
                    TaskOverviewVO.LastAnalysis lastAnalysis = toolLastAnalysisMap.get(toolLastAnalysisResultVO.getToolName());
                    lastAnalysis.setLastAnalysisResult(toolLastAnalysisResultVO.getLastAnalysisResultVO());
                    long elapseTime = toolLastAnalysisResultVO.getElapseTime();
                    long endTime = toolLastAnalysisResultVO.getEndTime();
                    long startTime = toolLastAnalysisResultVO.getStartTime();
                    long lastAnalysisTime = endTime;
                    if (elapseTime == 0 && endTime != 0)
                    {
                        elapseTime = endTime - startTime;
                    }

                    if (endTime == 0)
                    {
                        lastAnalysisTime = startTime;
                    }
                    lastAnalysis.setElapseTime(elapseTime);
                    lastAnalysis.setLastAnalysisTime(lastAnalysisTime);
                }
            }
            String orderToolIds = commonDao.getToolOrder();
            List<String> toolOrderList = Arrays.asList(orderToolIds.split(","));

            toolLastAnalysisList.sort(Comparator.comparingInt(o -> toolOrderList.indexOf(o.getToolName()))
            );

            taskOverviewVO.setLastAnalysisResultList(toolLastAnalysisList);
        }
        return taskOverviewVO;
    }


    /**
     * 开启任务
     *
     * @param taskId
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = ENABLE_ACTION)
    public Boolean startTask(Long taskId, String userName)
    {
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        List<String> taskMemberList = taskEntity.getTaskMember();
        List<String> taskOwnerList = taskEntity.getTaskOwner();
        Boolean taskMemberPermission = CollectionUtils.isEmpty(taskMemberList) || !taskMemberList.contains(userName);
        Boolean taskOwnerPermission = CollectionUtils.isEmpty(taskOwnerList) || !taskOwnerList.contains(userName);
        if(taskMemberPermission && taskOwnerPermission)
        {
            logger.error("current user has no permission to the task");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED);
        }


        if (CollectionUtils.isNotEmpty(taskEntity.getExecuteDate()) && StringUtils.isNotBlank(taskEntity.getExecuteTime()))
        {
            logger.error("The task is already open and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_START);
        }

        // 如果是蓝盾项目，要开启流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId()))
        {
            // 启动时，把原先的定时任务恢复
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo))
            {
                logger.error("pipeline execution timing is empty.");
                throw new CodeCCException(TaskMessageCode.PIPELINE_EXECUTION_TIME_EMPTY);
            }

            String lastExecuteTime = lastDisableTaskInfo.getLastExecuteTime();
            List<String> lastExecuteDate = lastDisableTaskInfo.getLastExecuteDate();

            // 开启定时执行的日期时间
            taskEntity.setExecuteTime(lastExecuteTime);
            taskEntity.setExecuteDate(lastExecuteDate);
            // 删除DB保存的执行时间
            taskEntity.setLastDisableTaskInfo(null);

            pipelineService.modifyCodeCCTiming(taskEntity, lastExecuteDate, lastExecuteTime, userName);
        }

        taskEntity.setDisableTime("");
        taskEntity.setDisableReason("");
        taskEntity.setStatus(TaskConstants.TaskStatus.ENABLE.value());

        //在权限中心重新注册任务
        bkAuthExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskId), taskEntity.getNameEn(), taskEntity.getProjectId());

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 停用任务
     *
     * @param taskId
     * @param disabledReason
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopTask(Long taskId, String disabledReason, String userName)
    {
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        if (StringUtils.isNotBlank(taskEntity.getDisableTime()))
        {
            logger.error("The task is already close and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_CLOSE);
        }

        // 如果是蓝盾项目，并且是服务创建的，要停止流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId()) && BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskEntity.getCreateFrom()))
        {
            String executeTime = taskEntity.getExecuteTime();
            List<String> executeDate = taskEntity.getExecuteDate();

            if (CollectionUtils.isEmpty(executeDate) && StringUtils.isBlank(executeTime))
            {
                logger.error("pipeline execution date and time is empty.");
                throw new CodeCCException(TaskMessageCode.PIPELINE_EXECUTION_TIME_EMPTY);
            }

            // 调用蓝盾API 删除定时构建原子
            pipelineService.deleteCodeCCTiming(userName, taskEntity.getCreateFrom(), taskEntity.getPipelineId(), taskEntity.getProjectId());

            // 存储启用日期时间到DisableTaskEntity
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo))
            {
                lastDisableTaskInfo = new DisableTaskEntity();
            }

            lastDisableTaskInfo.setLastExecuteTime(executeTime);
            lastDisableTaskInfo.setLastExecuteDate(executeDate);
            taskEntity.setLastDisableTaskInfo(lastDisableTaskInfo);
        }

        //要将权限中心的任务成员，任务管理员同步到task表下面，便于后续启用时再进行注册
        TaskMemberVO taskMemberVO = getTaskMemberAndAdmin(taskId, taskEntity.getProjectId());
        taskEntity.setTaskMember(taskMemberVO.getTaskMember());
        taskEntity.setTaskOwner(taskMemberVO.getTaskOwner());

        //在权限中心中删除相应的资源
        try
        {
            bkAuthExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), taskEntity.getProjectId());
        }
        catch (UnauthorizedException e)
        {
            logger.error("delete iam resource fail! error message: {}", e.getMessage());
            throw new CodeCCException(TaskMessageCode.CLOSE_TASK_FAIL);
        }

        logger.info("stopping task: delete pipeline scheduled atom and auth center resource success! project id: {}",
                taskEntity.getProjectId());

        taskEntity.setExecuteDate(new ArrayList<>());
        taskEntity.setExecuteTime("");
        taskEntity.setDisableTime(String.valueOf(System.currentTimeMillis()));
        taskEntity.setDisableReason(disabledReason);
        taskEntity.setStatus(TaskConstants.TaskStatus.DISABLE.value());

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 获取代码库配置信息
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskCodeLibraryVO getCodeLibrary(Long taskId)
    {
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 获取所有工具的基础信息
        Map<String, ToolMetaBaseVO> toolMetaMap = toolMetaCache.getToolMetaListFromCache(Boolean.FALSE, Boolean.TRUE);

        // 获取排序好的所有工具
        String[] toolIdArr = Optional.ofNullable(taskEntity.getToolNames())
                .map(tool -> tool.split(ComConstants.STRING_SPLIT))
                .orElse(new String[]{});

        // 获取工具配置Map
        Map<String, JSONObject> chooseJsonMap = getToolConfigInfoMap(taskEntity);
        List<ToolConfigParamJsonVO> paramJsonList = new ArrayList<>();
        for (String toolName : toolIdArr)
        {
            // 工具被禁用则不显示
            if (!chooseJsonMap.keySet().contains(toolName))
            {
                continue;
            }

            // 获取工具对应的基本数据
            ToolMetaBaseVO toolMetaBaseVO = toolMetaMap.get(toolName);

            if (Objects.nonNull(toolMetaBaseVO))
            {
                String params = toolMetaBaseVO.getParams();
                if (StringUtils.isNotBlank(params) && !ComConstants.STRING_NULL_ARRAY.equals(params))
                {
                    JSONObject chooseJson = chooseJsonMap.get(toolName);
                    List<Map<String, Object>> arrays = JsonUtil.INSTANCE.to(params);
                    for (Map<String, Object> array : arrays)
                    {
                        ToolConfigParamJsonVO toolConfig = JsonUtil.INSTANCE.mapTo(array, ToolConfigParamJsonVO.class);
                        String toolChooseValue = Objects.isNull(chooseJson) ?
                                toolConfig.getVarDefault() : StringUtils.isBlank((String) chooseJson.get(toolConfig.getVarName())) ?
                                toolConfig.getVarDefault() : (String) chooseJson.get(toolConfig.getVarName());

                        toolConfig.setTaskId(taskId);
                        toolConfig.setToolName(toolMetaBaseVO.getName());
                        toolConfig.setChooseValue(toolChooseValue);
                        paramJsonList.add(toolConfig);
                    }
                }
            }
        }

        TaskCodeLibraryVO taskCodeLibrary = new TaskCodeLibraryVO();
        BeanUtils.copyProperties(taskEntity, taskCodeLibrary);
        taskCodeLibrary.setToolConfigList(paramJsonList);

        if (StringUtils.isBlank(taskCodeLibrary.getBranch()))
        {
            taskCodeLibrary.setBranch("master");
        }

        return taskCodeLibrary;
    }


    @Override
    public Boolean checkTaskExists(long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        if (null == taskInfoEntity)
        {
            return false;
        }
        List<BkAuthExResourceActionModel> validateResult = bkAuthExPermissionApi.validateBatchPermission(taskInfoEntity.getTaskOwner().get(0), String.valueOf(taskInfoEntity.getTaskId()),
                taskInfoEntity.getProjectId(), new ArrayList<BkAuthExAction>()
                {{
                    add(BkAuthExAction.TASK_OWNER);
                }});
        if (CollectionUtils.isEmpty(validateResult))
        {
            logger.info("validate result is empty! task id: {}", taskId);
            return false;
        }
        for (BkAuthExResourceActionModel bkAuthExResourceActionModel : validateResult)
        {
            if (true == bkAuthExResourceActionModel.isPass())
            {
                return true;
            }
        }
        return false;

    }


    /**
     * 更新代码库信息
     *
     * @param taskId
     * @param taskCodeLibrary
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_CODE_REPOSITORY, operType = MODIFY_INFO)
    public Boolean updateCodeLibrary(Long taskId, String userName, TaskCodeLibraryVO taskCodeLibrary)
    {
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 更新工具配置信息
        updateToolConfigInfoEntity(taskCodeLibrary, taskEntity, userName);

        taskEntity.setRepoHashId(taskCodeLibrary.getRepoHashId());
        taskEntity.setBranch(taskCodeLibrary.getBranch());
        taskEntity.setScmType(taskCodeLibrary.getScmType());

        BatchRegisterVO registerVO = new BatchRegisterVO();
        registerVO.setRepositoryHashId(taskEntity.getRepoHashId());
        registerVO.setBranchName(taskEntity.getBranch());
        registerVO.setScmType(taskEntity.getScmType());

        //更新流水线设置
        Element codeElement = pipelineService.getCodeElement(registerVO, getRelPath(taskCodeLibrary.getToolConfigList()));
        pipelineService.updatePipelineTools(userName, taskId, Collections.EMPTY_LIST, taskEntity, PipelineToolUpdateType.GET, codeElement);

        return taskDao.updateEntity(taskEntity, userName);
    }


    private String getRelPath(List<ToolConfigParamJsonVO> toolConfigList)
    {
        if (CollectionUtils.isNotEmpty(toolConfigList))
        {
            for (ToolConfigParamJsonVO toolConfigParamJsonVO : toolConfigList)
            {
                if (Tool.GOML.name().equalsIgnoreCase(toolConfigParamJsonVO.getToolName()))
                {
                    if (ComConstants.PARAMJSON_KEY_REL_PATH.equalsIgnoreCase(toolConfigParamJsonVO.getVarName()))
                    {
                        return toolConfigParamJsonVO.getChooseValue();
                    }
                }
            }
        }
        return "";
    }


    /**
     * 获取任务成员及管理员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    @Override
    public TaskMemberVO getTaskMemberAndAdmin(long taskId, String projectId)
    {
        Map<String, List<String>> taskMemberMap = bkAuthExPermissionApi.queryUserListForAction(String.valueOf(taskId), projectId, new ArrayList<BkAuthExAction>()
        {{
            add(BkAuthExAction.TASK_MEMBER);
            add(BkAuthExAction.TASK_OWNER);
        }});
        TaskMemberVO taskMemberVO = new TaskMemberVO();
        taskMemberVO.setTaskMember(taskMemberMap.get(BkAuthExAction.TASK_MEMBER.getActionName()));
        taskMemberVO.setTaskOwner(taskMemberMap.get(BkAuthExAction.TASK_OWNER.getActionName()));
        return taskMemberVO;
    }


    @Override
    @OperationHistory(funcId = FUNC_TRIGGER_ANALYSIS, operType = TRIGGER_ANALYSIS)
    public Boolean manualExecuteTask(long taskId, String isFirstTrigger, String userName)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList))
        {
            logger.info("tool list is empty! task id: {}", taskId);
            return false;
        }

        Set<String> toolSet = toolConfigInfoEntityList.stream().filter(toolConfigInfoEntity ->
                FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()
        ).map(ToolConfigInfoEntity::getToolName
        ).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(toolSet))
        {
            //停止原有正在运行的流水线
            Result<Boolean> stopResult = client.get(ServiceTaskLogRestResource.class).stopRunningTask(taskInfoEntity.getPipelineId(), taskInfoEntity.getNameEn(),
                    toolSet, taskInfoEntity.getProjectId(), taskId, userName);
            if (stopResult.isNotOk() || null == stopResult.getData() || !stopResult.getData())
            {
                logger.error("stop running pipeline fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            //启动流水线
            String buildId = pipelineService.startPipeline(taskInfoEntity, new ArrayList<>(toolSet), userName);
            //更新任务状态
            toolSet.forEach(tool ->
                    pipelineService.updateTaskInitStep(isFirstTrigger, taskInfoEntity, buildId, tool, userName)
            );
        }
        return true;
    }


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @return
     */
    @Override
    public TaskDetailVO getTaskInfoByPipelineId(String pipelineId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByPipelineId(pipelineId);
        TaskDetailVO taskDetailVO = new TaskDetailVO();
        if (Objects.nonNull(taskInfoEntity))
        {
            BeanUtils.copyProperties(taskInfoEntity, taskDetailVO);
        }
        return taskDetailVO;
    }

    @Override
    public TaskStatusVO getTaskStatus(Long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        if(null == taskInfoEntity)
        {
            return null;
        }
        return new TaskStatusVO(taskInfoEntity.getStatus());
    }


    /**
     * 获取所有的基础工具信息
     *
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache()
    {
        return toolMetaCache.getToolMetaListFromCache(Boolean.FALSE, Boolean.FALSE);
    }


    /**
     * 更新工具配置信息
     *
     * @param taskCodeLibrary
     * @param taskEntity
     */
    private void updateToolConfigInfoEntity(TaskCodeLibraryVO taskCodeLibrary, TaskInfoEntity taskEntity, String userName)
    {
        // 获取当前任务的工具的配置信息
        List<ToolConfigInfoEntity> toolConfigList = taskEntity.getToolConfigInfoList();
        // 提交更新任务工具的配置信息
        List<ToolConfigParamJsonVO> updateToolConfigList = taskCodeLibrary.getToolConfigList();

        if (CollectionUtils.isNotEmpty(toolConfigList) && CollectionUtils.isNotEmpty(updateToolConfigList))
        {
            for (ToolConfigParamJsonVO config : updateToolConfigList)
            {
                // 从工具配置信息中找对应的工具，找不到则不更新
                ToolConfigInfoEntity toolConfigInfoEntity = toolConfigList.stream()
                        .filter(tool -> tool.getToolName().equals(config.getToolName()))
                        .findFirst().orElse(null);

                JSONObject updateJson = new JSONObject();
                String varName = config.getVarName();
                String updateValue = config.getChooseValue();

                if (Objects.nonNull(toolConfigInfoEntity))
                {
                    // 需要更新的字段param_json
                    String paramJson = toolConfigInfoEntity.getParamJson();
                    if (StringUtils.isBlank(paramJson))
                    {
                        // 为空则构造Json, 如: {"phpcs_standard":"Generic"}
                        updateJson.put(varName, updateValue);
                    }
                    else
                    {
                        // 解析当前DB中的的选择参数
                        updateJson = JSONObject.fromObject(paramJson);
                        String orgValue = (String) updateJson.get(varName);

                        // 提交的值等于数据库的值，默认不更新
                        if (updateValue.equals(orgValue))
                        {
                            continue;
                        }
                        // 有对应的key才更新
                        updateJson.put(varName, updateValue);
                    }

                    toolConfigInfoEntity.setParamJson(JSONUtils.valueToString(updateJson));
                    toolRepository.save(toolConfigInfoEntity);
//                    toolDao.updateParamJson(toolConfigInfoEntity, userName);

                }
                else
                {
                    // 设置工具信息实体类 [ 工具第一次设置值, 比如文本框默认为空，提交后才会有值到toolConfigInfoEntity中 ]
                    String toolName = config.getToolName();
                    toolConfigInfoEntity = new ToolConfigInfoEntity();
                    toolConfigInfoEntity.setTaskId(taskEntity.getTaskId());
                    toolConfigInfoEntity.setToolName(toolName);
                    updateJson.put(varName, updateValue);
                    toolConfigInfoEntity.setParamJson(JSONUtils.valueToString(updateJson));
                    toolRepository.save(toolConfigInfoEntity);

                    List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
                    if (CollectionUtils.isEmpty(toolConfigInfoList))
                    {
                        toolConfigInfoList = new ArrayList<>();
                    }
                    toolConfigInfoList.add(toolConfigInfoEntity);

                    // 修改任务信息
                    String toolNames = taskEntity.getToolNames();
                    if (StringUtils.isBlank(toolNames))
                    {
                        toolNames = toolName;
                    }
                    else
                    {
                        if (!toolNames.contains(toolName))
                        {
                            toolNames = String.format("%s,%s", toolNames, toolName);
                        }
                    }
                    taskEntity.setToolNames(toolNames);
                    taskEntity.setToolConfigInfoList(toolConfigInfoList);
                    taskRepository.save(taskEntity);
                }
            }
        }
    }


    /**
     * 获取工具配置Map
     *
     * @param taskEntity
     * @return
     */
    @NotNull
    private Map<String, JSONObject> getToolConfigInfoMap(TaskInfoEntity taskEntity)
    {
        // 获取工具配置的值
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        Map<String, JSONObject> chooseJsonMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolConfigInfoList))
        {
            // 排除下架的工具
            toolConfigInfoList.stream()
                    .filter(config -> config.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                    .forEach(config -> chooseJsonMap.put(config.getToolName(), JSONObject.fromObject(config.getParamJson())));
        }
        return chooseJsonMap;
    }


    /**
     * 判断提交的参数是否为空
     *
     * @param taskUpdateVO
     * @return
     */
    private Boolean checkParam(TaskUpdateVO taskUpdateVO)
    {
        if (StringUtils.isBlank(taskUpdateVO.getNameCn()))
        {
            return false;
        }
        return taskUpdateVO.getCodeLang() > 0;
    }


    /**
     * 给工具分类及排序
     *
     * @param taskBaseVO
     * @param toolEntityList
     */
    private void sortedToolList(TaskBaseVO taskBaseVO, List<ToolConfigInfoEntity> toolEntityList)
    {
        // 如果工具不为空，对工具排序并且赋值工具展示名
        if (CollectionUtils.isNotEmpty(toolEntityList))
        {
            List<ToolConfigBaseVO> enableToolList = new ArrayList<>();
            List<ToolConfigBaseVO> disableToolList = new ArrayList<>();
            Map<String, ToolMetaBaseVO> toolMetaMap = toolMetaCache.getToolMetaListFromCache(false, true);

            final String toolIdsOrder = commonDao.getToolOrder();
            String[] toolIDArr = toolIdsOrder.split(",");
            for (String toolName : toolIDArr)
            {
                for (ToolConfigInfoEntity toolEntity : toolEntityList)
                {
                    if (toolName.equals(toolEntity.getToolName()) && toolMetaMap.get(toolName) != null)
                    {
                        ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
                        BeanUtils.copyProperties(toolEntity, toolConfigBaseVO);
                        toolConfigBaseVO.setToolDisplayName(toolMetaMap.get(toolName).getDisplayName());
                        toolConfigBaseVO.setToolPattern(toolMetaMap.get(toolName).getPattern());

                        if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigBaseVO.getFollowStatus())
                        {
                            disableToolList.add(toolConfigBaseVO);
                        }
                        else
                        {
                            enableToolList.add(toolConfigBaseVO);
                        }
                    }
                }
            }

            taskBaseVO.setEnableToolList(enableToolList);
            taskBaseVO.setDisableToolList(disableToolList);
        }
    }


    /**
     * 重置工具的顺序，数据库中工具是按接入的先后顺序排序的，前端展示要按照工具类型排序
     *
     * @param toolNames
     * @param toolIdsOrder
     * @return
     */
    private String resetToolOrderByType(String toolNames, final String toolIdsOrder, List<ToolConfigInfoVO> unsortedToolList,
                                        List<ToolConfigInfoVO> sortedToolList)
    {
        if (StringUtils.isEmpty(toolNames))
        {
            return null;
        }

        String[] toolNamesArr = toolNames.split(",");
        List<String> originToolList = Arrays.asList(toolNamesArr);
        String[] toolIDArr = toolIdsOrder.split(",");
        List<String> orderToolList = Arrays.asList(toolIDArr);
        Iterator<String> it = orderToolList.iterator();
        StringBuffer sb = new StringBuffer();
        while (it.hasNext())
        {
            String toolId = it.next();
            if (originToolList.contains(toolId))
            {
                sb.append(toolId).append(",");
                List<ToolConfigInfoVO> filteredList = unsortedToolList.stream().
                        filter(toolConfigInfoVO ->
                                toolId.equalsIgnoreCase(toolConfigInfoVO.getToolName())
                        ).
                        collect(Collectors.toList());

                sortedToolList.addAll(CollectionUtils.isNotEmpty(filteredList) ? filteredList : Collections.EMPTY_LIST);
            }
        }
        if (sb.length() > 0)
        {
            toolNames = sb.substring(0, sb.length() - 1);
        }

        return toolNames;
    }


    private TaskListVO sortByDate(List<TaskDetailVO> taskDetailVOS)
    {
        TaskListVO taskList = new TaskListVO(Collections.emptyList(), Collections.emptyList());
        if (CollectionUtils.isNotEmpty(taskDetailVOS))
        {
            //分离已启用项目和停用项目
            List<TaskDetailVO> enableProjs = new ArrayList<>();
            List<TaskDetailVO> disableProjs = new ArrayList<>();
            for (TaskDetailVO taskDetailVO : taskDetailVOS)
            {
                if (TaskConstants.TaskStatus.DISABLE.value() != taskDetailVO.getStatus())
                {
                    enableProjs.add(taskDetailVO);
                }
                else
                {
                    disableProjs.add(taskDetailVO);
                }
            }

            //启用的项目按创建时间倒排
            enableProjs.sort((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()));

            //应用的项目按停用时间倒排
            disableProjs.sort((o1, o2) -> (StringUtils.isEmpty(o2.getDisableTime()) ? Long.valueOf(0) : Long.valueOf(o2.getDisableTime()))
                    .compareTo(StringUtils.isEmpty(o1.getDisableTime()) ? Long.valueOf(0) : Long.valueOf(o1.getDisableTime())));

            //重建projectList
            taskList.setEnableTasks(enableProjs);
            taskList.setDisableTasks(disableProjs);
        }
        return taskList;
    }



}
