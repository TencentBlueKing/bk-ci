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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.bk.codecc.defect.service.PipelineTaskService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.StepFlag;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 任务分析记录服务实现方法
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
public class TaskLogServiceImpl implements TaskLogService {
    private static Logger logger = LoggerFactory.getLogger(TaskLogServiceImpl.class);

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private TaskLogDao taskLogDao;

    @Autowired
    private BizServiceFactory<IQueryStatisticBizService> taskLogAndDefectFactory;

    @Autowired
    private PipelineTaskService pipelineTaskService;

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private Client client;

    @Autowired
    private BuildRepository buildRepository;

    @Override
    public TaskLogVO getLatestTaskLog(long taskId, String toolName) {
        TaskLogEntity taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameOrderByStartTimeDesc(taskId, toolName);
        TaskLogVO taskLogVO = new TaskLogVO();
        BeanUtils.copyProperties(taskLogEntity, taskLogVO);
        return taskLogVO;
    }

    @Override
    public List<ToolLastAnalysisResultVO> getLastTaskLogResult(long taskId, Set<String> toolSet) {
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolSet)) {
            return toolLastAnalysisResultVOList;
        }
        List<TaskLogGroupEntity> taskLogGroupEntities = taskLogDao.findFirstByTaskIdOrderByStartTime(taskId, toolSet);
        if (CollectionUtils.isNotEmpty(taskLogGroupEntities)) {
            for (TaskLogGroupEntity taskLogGroupEntity : taskLogGroupEntities) {
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogGroupEntity, toolLastAnalysisResultVO);
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO);
            }
        }
        return toolLastAnalysisResultVOList;
    }

    @Override
    public List<ToolLastAnalysisResultVO> getLastAnalysisResults(long taskId, Set<String> toolSet) {
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();

        if (CollectionUtils.isEmpty(toolSet)) {
            return toolLastAnalysisResultVOList;
        }
        List<TaskLogGroupEntity> taskLogGroupEntities = taskLogDao.findFirstByTaskIdOrderByStartTime(taskId, toolSet);
        if (CollectionUtils.isNotEmpty(taskLogGroupEntities)) {
            for (TaskLogGroupEntity taskLogGroupEntity : taskLogGroupEntities) {
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogGroupEntity, toolLastAnalysisResultVO);
                BaseLastAnalysisResultVO lastAnalysisResultVO = getLastAnalysisResult(toolLastAnalysisResultVO,
                        toolLastAnalysisResultVO.getToolName());
                toolLastAnalysisResultVO.setLastAnalysisResultVO(lastAnalysisResultVO);
                toolLastAnalysisResultVO.setFlag(taskLogGroupEntity.getFlag());
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO);
            }
        }
        return toolLastAnalysisResultVOList;
    }

    @Override public List<ToolLastAnalysisResultVO> getAnalysisResultsByBuildId(long taskId, String buildId) {
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();

        List<TaskLogEntity> taskLogEntityList = taskLogRepository.findByTaskIdAndBuildId(taskId, buildId);
        if (CollectionUtils.isNotEmpty(taskLogEntityList)) {
            for (TaskLogEntity taskLogEntity : taskLogEntityList) {
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogEntity, toolLastAnalysisResultVO);
                BaseLastAnalysisResultVO lastAnalysisResultVO =
                        getAnalysisResult(toolLastAnalysisResultVO, taskLogEntity.getToolName());
                toolLastAnalysisResultVO.setLastAnalysisResultVO(lastAnalysisResultVO);
                logger.info("metrics get task log: {}", toolLastAnalysisResultVO.getToolName());
                toolLastAnalysisResultVO.setToolName(taskLogEntity.getToolName());
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO);
            }
        }
        return toolLastAnalysisResultVOList;
    }

    @Override
    public List<ToolLastAnalysisResultVO> getAnalysisResults(long taskId, String buildNum) {
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();

        List<TaskLogEntity> taskLogEntityList = taskLogRepository.findByTaskIdAndBuildNum(taskId, buildNum);
        if (CollectionUtils.isNotEmpty(taskLogEntityList)) {
            for (TaskLogEntity taskLogEntity : taskLogEntityList) {
                if (Tool.SCC.name().equals(taskLogEntity.getToolName())) {
                    continue;
                }
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogEntity, toolLastAnalysisResultVO);
                BaseLastAnalysisResultVO lastAnalysisResultVO =
                        getAnalysisResult(toolLastAnalysisResultVO, taskLogEntity.getToolName());
                toolLastAnalysisResultVO.setLastAnalysisResultVO(lastAnalysisResultVO);
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO);
            }
        }
        return toolLastAnalysisResultVOList;
    }

    /**
     * 获取最近一次分析记录
     *
     * @param toolLastAnalysisResultVO
     * @param toolName
     * @return
     */
    @Override
    public BaseLastAnalysisResultVO getLastAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO,
                                                          String toolName) {
        logger.info("begin to query last analysis result, task id: {}, tool name : {}",
                toolLastAnalysisResultVO.getTaskId(),
                toolLastAnalysisResultVO.getToolName());
        IQueryStatisticBizService queryStatisticBizService = taskLogAndDefectFactory
                .createBizService(toolLastAnalysisResultVO.getToolName(),
                        ComConstants.BusinessType.QUERY_STATISTIC.value(), IQueryStatisticBizService.class);
        return queryStatisticBizService.processBiz(toolLastAnalysisResultVO, true);
    }

    /**
     * 获取某一次分析记录
     *
     * @param toolLastAnalysisResultVO
     * @param toolName
     * @return
     */
    @Override
    public BaseLastAnalysisResultVO getAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO,
                                                      String toolName) {
        logger.info("begin to query analysis result, task id: {}, tool name : {}, build num: {}",
                toolLastAnalysisResultVO.getTaskId(),
                toolLastAnalysisResultVO.getToolName(), toolLastAnalysisResultVO.getBuildNum());
        IQueryStatisticBizService queryStatisticBizService =
                taskLogAndDefectFactory.createBizService(toolLastAnalysisResultVO.getToolName(),
                        ComConstants.BusinessType.QUERY_STATISTIC.value(), IQueryStatisticBizService.class);
        return queryStatisticBizService.processBiz(toolLastAnalysisResultVO, false);
    }

    @Override
    public Boolean uploadDirStructSuggestParam(UploadTaskLogStepVO uploadTaskLogStepVO) {
        TaskLogEntity lastTaskLog = taskLogRepository.findFirstByTaskIdAndToolNameOrderByStartTimeDesc(
                uploadTaskLogStepVO.getTaskId(), uploadTaskLogStepVO.getToolName());
        List<TaskLogEntity.TaskUnit> stepArray = lastTaskLog.getStepArray();
        if (CollectionUtils.isNotEmpty(stepArray)) {
            for (TaskLogEntity.TaskUnit taskUnit : stepArray) {
                int stepNum = taskUnit.getStepNum();
                if (stepNum == ComConstants.Step4MutliTool.SCAN.value()) {
                    taskUnit.setDirStructSuggestParam(String.valueOf(uploadTaskLogStepVO.getDirStructSuggestParam()));
                    taskUnit.setCompileResult(String.valueOf(uploadTaskLogStepVO.getCompileResult()));
                }
            }
        }
        taskLogRepository.save(lastTaskLog);
        return true;
    }

    @Override
    public Boolean stopRunningTask(String projectId, String pipelineId, String streamName, long taskId, Set<String> toolSet, String userName) {
        List<TaskLogGroupEntity> taskLogGroupEntityList = taskLogDao.findFirstByTaskIdOrderByStartTime(taskId, toolSet);
        if (CollectionUtils.isNotEmpty(taskLogGroupEntityList)) {
            //1. 选取buildid停止流水线运行
            List<TaskLogGroupEntity> filteredTaskLogList = taskLogGroupEntityList.stream().
                    filter(taskLogGroupEntity ->
                            (taskLogGroupEntity.getEndTime() <= 0L)
                    ).
                    collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(filteredTaskLogList)) {
                List<Future> asyncResultList = new ArrayList<>();
                Set<String> buildIdSet = new HashSet<>();
                filteredTaskLogList.forEach(filteredTaskLog ->
                        {
                            Boolean stopFlag;
                            if (!buildIdSet.contains(filteredTaskLog.getBuildId()) && StringUtils.isNotEmpty(filteredTaskLog.getBuildId())) {
                                logger.info("current build id: {}", filteredTaskLog.getBuildId());
                                stopFlag = true;
                                buildIdSet.add(filteredTaskLog.getBuildId());
                            } else {
                                stopFlag = false;
                            }
                            asyncResultList.add(pipelineTaskService.handleStopTask(projectId, pipelineId, taskId, filteredTaskLog, userName, stopFlag, streamName));
                        }
                );
                asyncResultList.forEach(asyncResult ->
                        {
                            try {
                                asyncResult.get();
                            } catch (InterruptedException | ExecutionException e) {
                                logger.error("stop task fail!");
                            }
                        }
                );

            }
            logger.info("stop pipeline task and update task status success! task id:{}", taskId);
        }
        return true;
    }

    @Override
    public TaskLogVO getBuildTaskLog(long taskId, String toolName, String buildId) {
        logger.info("begin getBuildTaskLog: {}, {}, {}", taskId, toolName, buildId);
        TaskLogVO taskLogVO = new TaskLogVO();
        List<TaskLogVO.TaskUnit> taskUnits = Lists.newArrayList();
        TaskLogEntity taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (taskLogEntity != null) {
            if (CollectionUtils.isNotEmpty(taskLogEntity.getStepArray())) {
                for (TaskLogEntity.TaskUnit taskEntityUnit : taskLogEntity.getStepArray()) {
                    TaskLogVO.TaskUnit taskVOUnit = new TaskLogVO.TaskUnit();
                    BeanUtils.copyProperties(taskEntityUnit, taskVOUnit);
                    taskUnits.add(taskVOUnit);
                }
            }
            BeanUtils.copyProperties(taskLogEntity, taskLogVO);
        } else {
            taskLogVO.setTaskId(taskId);
            taskLogVO.setToolName(toolName);
            taskLogVO.setBuildId(buildId);
        }
        taskLogVO.setStepArray(taskUnits);

        logger.info("end getBuildTaskLog");
        return taskLogVO;
    }

    @Override
    public List<TaskLogVO> getBuildTaskLog(long taskId, List<String> toolNameSet, String buildId) {
        logger.info("begin getBuildTaskLog: {}, {}, {}", taskId, toolNameSet, buildId);
        List<TaskLogEntity> taskLogEntityList = taskLogRepository.findByTaskIdAndToolNameInAndBuildId(taskId, toolNameSet, buildId);

        Map<String, TaskLogVO> taskLogVOMap = new HashMap<>();
        toolNameSet.forEach((it) -> {
            TaskLogVO taskLogVO = new TaskLogVO();
            taskLogVO.setTaskId(taskId);
            taskLogVO.setToolName(it);
            taskLogVO.setBuildId(buildId);
            taskLogVO.setStepArray(Lists.newArrayList());
            taskLogVOMap.put(it, taskLogVO);
        });

        taskLogEntityList.forEach(taskLogEntity -> {
            TaskLogVO taskLogVo = taskLogVOMap.get(taskLogEntity.getToolName());

            List<TaskLogVO.TaskUnit> taskUnits = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(taskLogEntity.getStepArray())) {
                for (TaskLogEntity.TaskUnit taskEntityUnit : taskLogEntity.getStepArray()) {
                    TaskLogVO.TaskUnit taskVOUnit = new TaskLogVO.TaskUnit();
                    BeanUtils.copyProperties(taskEntityUnit, taskVOUnit);
                    taskUnits.add(taskVOUnit);
                }
            }
            BeanUtils.copyProperties(taskLogEntity, taskLogVo);
            taskLogVo.setStepArray(taskUnits);
        });

        logger.info("end getBuildTaskLog");
        return new ArrayList<>(taskLogVOMap.values());
    }

    @Override
    public Result uploadTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO) {
        logger.info("recv a task step, step: {}, flag: {}, start: {}, end: {}",
                uploadTaskLogStepVO.getStepNum(), uploadTaskLogStepVO.getFlag(), uploadTaskLogStepVO.getStartTime(), uploadTaskLogStepVO.getEndTime());
        IBizService taskLogService = bizServiceFactory.createBizService(uploadTaskLogStepVO.getToolName(),
                ComConstants.BusinessType.ANALYZE_TASK.value(), IBizService.class);
        return taskLogService.processBiz(uploadTaskLogStepVO);
    }


    @Override
    public Boolean refreshTaskLogByPipeline(Long taskId, Set<String> toolNames) {
        if (CollectionUtils.isEmpty(toolNames)) {
            logger.error("tool names set is empty!");
            return false;
        }
        Result<TaskDetailVO> taskDetailVOResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskDetailVOResult.getData();
        if (taskDetailVOResult.isNotOk() || null == taskDetailVOResult.getData()) {
            logger.error("get task info failed! task id: {}", taskId);
            return false;
        }
        for (String toolName : toolNames) {
            UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
            uploadTaskLogStepVO.setTaskId(taskId);
            uploadTaskLogStepVO.setToolName(toolName);
            uploadTaskLogStepVO.setStreamName(taskDetailVO.getNameEn());
            Long currentTime = System.currentTimeMillis();
            uploadTaskLogStepVO.setStartTime(currentTime);
            uploadTaskLogStepVO.setEndTime(currentTime);
            uploadTaskLogStepVO.setFlag(StepFlag.FAIL.value());
            uploadTaskLogStepVO.setMsg("流水线运行异常！");
            //无论lint类或者cov都是在准备阶段报异常
            uploadTaskLogStepVO.setStepNum(0);
            uploadTaskLogStepVO.setPipelineFail(true);
            IBizService taskLogService = bizServiceFactory.createBizService(uploadTaskLogStepVO.getToolName(),
                    ComConstants.BusinessType.ANALYZE_TASK.value(), IBizService.class);
            taskLogService.processBiz(uploadTaskLogStepVO);
        }
        return true;
    }

    /**
     * 查询任务构建列表
     *
     * @param taskId
     * @param limit
     * @return
     */
    @Override
    public List<BuildVO> getTaskBuildInfos(long taskId, int limit) {
        List<TaskLogEntity> taskLogEntities = taskLogRepository.findByTaskId(taskId);
        Map<String, BuildVO> buildVOMap = Maps.newTreeMap(((o1, o2) -> Integer.valueOf(o2).compareTo(Integer.valueOf(o1))));
        if (CollectionUtils.isNotEmpty(taskLogEntities)) {
            for (TaskLogEntity taskLogEntity : taskLogEntities) {
                String taskBuildNum = taskLogEntity.getBuildNum();
                if (StringUtils.isNotEmpty(taskBuildNum) && buildVOMap.get(taskBuildNum) == null) {
                    BuildVO buildVO = new BuildVO();
                    buildVO.setBuildId(taskLogEntity.getBuildId());
                    buildVO.setBuildNum(taskLogEntity.getBuildNum());
                    buildVOMap.put(taskLogEntity.getBuildNum(), buildVO);
                }
            }
        }

        List<BuildVO> buildVOS = Lists.newArrayList(buildVOMap.values());
        if (buildVOS.size() > limit) {
            buildVOS = buildVOS.subList(0, limit);
        }

        List<String> buildIds = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(buildVOS)) {
            for (BuildVO buildVO : buildVOS) {
                buildIds.add(buildVO.getBuildId());
            }
        }
        if (CollectionUtils.isNotEmpty(buildIds)) {
            List<BuildEntity> buildEntities = buildRepository.findByBuildIdIn(buildIds);
            Map<String, BuildEntity> buildEntityMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(buildEntities)) {
                for (BuildEntity buildEntity : buildEntities) {
                    buildEntityMap.put(buildEntity.getBuildId(), buildEntity);
                }
            }
            for (BuildVO buildVO : buildVOS) {
                BuildEntity buildEntity = buildEntityMap.get(buildVO.getBuildId());
                if (buildEntity != null) {
                    buildVO.setBuildNum(buildEntity.getBuildNo());
                    buildVO.setBuildTime(buildEntity.getBuildTime());
                    buildVO.setBuildUser(buildEntity.getBuildUser());
                }
            }
        }
        return buildVOS;
    }

    @Override
    public List<TaskLogVO> batchTaskLogList(Set<Long> taskIds, String toolName) {
        List<TaskLogEntity> taskLogEntityList = taskLogDao.findLastTaskLogByTool(taskIds, toolName);
        return entity2TaskLogVos(taskLogEntityList);
    }

    @Override
    public List<TaskLogVO> batchTaskLogListByTime(Set<Long> taskIdSet, Long startTime, Long endTime) {
        List<TaskLogEntity> taskLogEntityList = taskLogDao.findTaskLogByTime(taskIdSet, startTime, endTime);
        return entity2TaskLogVos(taskLogEntityList);
    }

    @Override
    public Map<String, TaskLogRepoInfoVO> getLastAnalyzeRepoInfo(long taskId) {
        TaskLogEntity lastAnalyze = taskLogRepository.findFirstByTaskIdAndFlagOrderByStartTimeDesc(taskId,
                StepFlag.SUCC.value());
        if (lastAnalyze == null) {
            logger.warn("this task has been not ran, taskId: {}", taskId);
            return Collections.emptyMap();
        }

        Map<String, TaskLogRepoInfoVO> repoInfo = new HashMap<>();
        String buildId = lastAnalyze.getBuildId();
        List<TaskLogEntity> lastAnalyzeList = taskLogRepository.findByTaskIdAndBuildId(taskId, buildId);
        lastAnalyzeList.forEach(taskLogEntity -> {
            List<TaskLogEntity.TaskUnit> steps = taskLogEntity.getStepArray();
            steps.forEach(taskUnit -> {
                String msg = taskUnit.getMsg();
                if (StringUtils.isNotBlank(msg) && msg.contains("代码库：")) {
                    String[] msgs = msg.split("\n");
                    List<String> msgList = Arrays.asList(msgs);
                    msgList.stream()
                            .filter(StringUtils::isNotBlank)
                            .forEach(s -> {
                                try {
                                    String repoUrl = s.substring(s.indexOf("代码库：") + 4, s.indexOf("，版本号："));
                                    String revision = s.substring(s.indexOf("版本号：") + 4, s.indexOf("，提交时间"));
                                    String commitTime = s.substring(s.indexOf("提交时间：") + 5, s.indexOf("，提交人"));
                                    String commitUser = s.substring(s.indexOf("提交人：") + 4, s.indexOf("，分支"));
                                    String branch = s.substring(s.indexOf("分支：") + 3);
                                    TaskLogRepoInfoVO taskLogRepoInfoVO =
                                            new TaskLogRepoInfoVO(repoUrl, revision, commitTime, commitUser, branch);
                                    repoInfo.put(repoUrl, taskLogRepoInfoVO);
                                } catch (Throwable e) {
                                    logger.error("代码库信息截取失败: {}", msg);
                                    logger.error("", e);
                                }
                            });
                }
            });
        });
        return repoInfo;
    }

    @Override
    public TaskLogRepoInfoVO getLastAnalyzeRepoInfo(long taskId, String toolName) {
        TaskLogEntity lastAnalyze = taskLogRepository
                .findFirstByTaskIdAndToolNameOrderByStartTimeDesc(taskId, toolName);
        if (lastAnalyze == null) {
            logger.warn("this task has been not ran, taskId: {} | toolName: {}", taskId, toolName);
            return null;
        }

        String repoUrl = null;
        String revision = null;
        String commitTime = null;
        String commitUser = null;
        String branch = null;
        List<TaskLogEntity.TaskUnit> steps = lastAnalyze.getStepArray();
        for (TaskLogEntity.TaskUnit taskUnit : steps) {
            String msg = taskUnit.getMsg();
            if (StringUtils.isNotBlank(msg) && msg.contains("代码库：")) {
                try {
                    String[] msgs = msg.split("\n");
                    for (String s : msgs) {
                        repoUrl = s.substring(s.indexOf("代码库：") + 4, s.indexOf("，版本号："));
                        revision = s.substring(s.indexOf("版本号：") + 4, s.indexOf("，提交时间"));
                        commitTime = s.substring(s.indexOf("提交时间：") + 5, s.indexOf("，提交人"));
                        commitUser = s.substring(s.indexOf("提交人：") + 4, s.indexOf("，分支"));
                        branch = s.substring(s.indexOf("分支：") + 3);
                    }
                } catch (Exception e) {
                    logger.error("代码库信息截取失败: {}", msg);
                    e.printStackTrace();
                }
            }
        }

        return new TaskLogRepoInfoVO(repoUrl, revision, commitTime, commitUser, branch);
    }

    @Override
    public List<TaskLogVO> findLastBuildInfo(long taskId) {
        TaskLogEntity lastTaskLog =
                taskLogRepository.findFirstByTaskIdAndFlagOrderByStartTimeDesc(taskId, StepFlag.SUCC.value());
        List<TaskLogEntity> lastTaskLogList =
                taskLogRepository.findByTaskIdAndBuildId(taskId, lastTaskLog.getBuildId());
        List<TaskLogEntity> failTaskLogList = Lists.newLinkedList();
        for (TaskLogEntity taskLogEntity : lastTaskLogList) {
            if (taskLogEntity.getFlag() != StepFlag.SUCC.value()) {
                TaskLogEntity entity =
                        taskLogRepository.findFirstByTaskIdAndToolNameAndFlagOrderByStartTimeDesc(taskId,
                                taskLogEntity.getToolName(),
                                StepFlag.SUCC.value());
                failTaskLogList.add(entity);
            }
        }

        lastTaskLogList.addAll(failTaskLogList);
        lastTaskLogList = lastTaskLogList.stream()
                .filter(taskLogEntity -> taskLogEntity != null && taskLogEntity.getFlag() == StepFlag.SUCC.value())
                .collect(Collectors.toList());
        return entity2TaskLogVos(lastTaskLogList);
    }

    @Override
    public List<TaskLogVO> getCurrBuildInfo(long taskId, String buildId) {
        List<TaskLogEntity> taskLogList = taskLogRepository.findByTaskIdAndBuildId(taskId, buildId);
        return entity2TaskLogVos(taskLogList);
    }

    @NotNull
    private List<TaskLogVO> entity2TaskLogVos(List<TaskLogEntity> taskLogEntityList) {
        List<TaskLogVO> taskLogVoList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskLogEntityList)) {
            taskLogEntityList.forEach(taskLogEntity ->
            {
                TaskLogVO taskLogVO = new TaskLogVO();
                BeanUtils.copyProperties(taskLogEntity, taskLogVO);
                taskLogVoList.add(taskLogVO);
            });
        }

        return taskLogVoList;
    }

    @Override
    public Map<String, Boolean> defectCommitSuccess(long taskId, List<String> toolNameSet, String buildId, int stepNum)
    {
        Map<String, Boolean> resultMap = new HashMap<>();
        toolNameSet.forEach(it -> resultMap.put(it, false));

        List<TaskLogVO> taskLogVOList = getBuildTaskLog(taskId, toolNameSet, buildId);
        taskLogVOList.forEach(taskLogVO -> {
            if (CollectionUtils.isNotEmpty(taskLogVO.getStepArray()))
            {
                boolean matchResult = taskLogVO.getStepArray().stream().anyMatch(taskUnit ->
                        stepNum == taskUnit.getStepNum() && StepFlag.SUCC.value() == taskUnit.getFlag());
                resultMap.put(taskLogVO.getToolName(), matchResult);
            }
        });
        return resultMap;
    }
}
