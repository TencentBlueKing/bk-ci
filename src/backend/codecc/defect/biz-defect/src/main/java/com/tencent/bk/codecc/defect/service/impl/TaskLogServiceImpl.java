/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;
import com.tencent.bk.codecc.defect.service.*;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.api.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 任务分析记录服务实现方法
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
public class TaskLogServiceImpl implements TaskLogService
{
    private static Logger logger = LoggerFactory.getLogger(TaskLogServiceImpl.class);

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private TaskLogDao taskLogDao;

    @Autowired
    private BizServiceFactory<IQueryStatisticBizService> taskLogAndDefectFactory;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private SnapShotService snapShotService;

    @Autowired
    private PipelineTaskService pipelineTaskService;

    @Override
    public TaskLogVO getLatestTaskLog(long taskId, String toolName)
    {
        TaskLogEntity taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameOrderByStartTimeDesc(taskId, toolName);
        TaskLogVO taskLogVO = new TaskLogVO();
        BeanUtils.copyProperties(taskLogEntity, taskLogVO);
        return taskLogVO;
    }

    @Override
    public List<ToolLastAnalysisResultVO> getLastAnalysisResults(long taskId, Set<String> toolSet)
    {
        logger.info("start to get analysis result!");
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();

        if (CollectionUtils.isEmpty(toolSet))
        {
            return toolLastAnalysisResultVOList;
        }
        List<TaskLogGroupEntity> taskLogGroupEntities = taskLogDao.findFirstByTaskIdOrderByStartTime(taskId, toolSet);
        if (CollectionUtils.isNotEmpty(taskLogGroupEntities))
        {
            for (TaskLogGroupEntity taskLogGroupEntity : taskLogGroupEntities)
            {
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogGroupEntity, toolLastAnalysisResultVO);
                BaseLastAnalysisResultVO lastAnalysisResultVO = getLastAnalysisResult(toolLastAnalysisResultVO, toolLastAnalysisResultVO.getToolName());
                toolLastAnalysisResultVO.setLastAnalysisResultVO(lastAnalysisResultVO);
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO);
            }
        }
        logger.info("get last analysis! size is: {}", toolLastAnalysisResultVOList.size());
        return toolLastAnalysisResultVOList;
    }


    @Override
    public List<ToolLastAnalysisResultVO> getAnalysisResultsList(long taskId, String toolName)
    {
        List<ToolLastAnalysisResultVO> toolLastAnalysisResultVOList = new ArrayList<>();

        List<TaskLogEntity> taskLogEntityList = taskLogRepository.findByTaskIdAndToolNameOrderByStartTimeDesc(taskId, toolName);
        if (CollectionUtils.isNotEmpty(taskLogEntityList))
        {
            for (TaskLogEntity taskLogEntity : taskLogEntityList)
            {
                ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
                BeanUtils.copyProperties(taskLogEntity, toolLastAnalysisResultVO);
                BaseLastAnalysisResultVO lastAnalysisResultVO = getLastAnalysisResult(toolLastAnalysisResultVO, toolName);
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
    public BaseLastAnalysisResultVO getLastAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO, String toolName)
    {
        IQueryStatisticBizService queryStatisticBizService = taskLogAndDefectFactory.createBizService(toolLastAnalysisResultVO.getToolName(),
                ComConstants.BusinessType.QUERY_STATISTIC.value(), IQueryStatisticBizService.class);
        return queryStatisticBizService.processBiz(toolLastAnalysisResultVO);
    }


    @Override
    public Boolean uploadDirStructSuggestParam(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        TaskLogEntity lastTaskLog = taskLogRepository.findFirstByTaskIdAndToolNameOrderByStartTimeDesc(
                uploadTaskLogStepVO.getTaskId(), uploadTaskLogStepVO.getToolName());
        List<TaskLogEntity.TaskUnit> stepArray = lastTaskLog.getStepArray();
        if (CollectionUtils.isNotEmpty(stepArray))
        {
            for (TaskLogEntity.TaskUnit taskUnit : stepArray)
            {
                int stepNum = taskUnit.getStepNum();
                if (stepNum == ComConstants.Step4MutliTool.SCAN.value())
                {
                    taskUnit.setDirStructSuggestParam(String.valueOf(uploadTaskLogStepVO.getDirStructSuggestParam()));
                    taskUnit.setCompileResult(String.valueOf(uploadTaskLogStepVO.getCompileResult()));
                }
            }
        }
        taskLogRepository.save(lastTaskLog);
        return true;
    }

    @Override
    public Boolean stopRunningTask(String projectId, String pipelineId, String streamName, long taskId, Set<String> toolSet, String userName)
    {
        List<TaskLogGroupEntity> taskLogGroupEntityList = taskLogDao.findFirstByTaskIdOrderByStartTime(taskId, toolSet);
        if (CollectionUtils.isNotEmpty(taskLogGroupEntityList))
        {
            //1. 选取buildid停止流水线运行
            List<TaskLogGroupEntity> filteredTaskLogList = taskLogGroupEntityList.stream().
                    filter(taskLogGroupEntity ->
                            (taskLogGroupEntity.getCurrStep() != ComConstants.Step4MutliTool.COMPLETE.value() &&
                                    taskLogGroupEntity.getCurrStep() != ComConstants.Step4MutliTool.READY.value())
                    ).
                    collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(filteredTaskLogList))
            {
                List<Future> asyncResultList = new ArrayList<>();
                Set<String> buildIdSet = new HashSet<>();
                filteredTaskLogList.forEach(filteredTaskLog ->
                        {
                            Boolean stopFlag;
                            if (!buildIdSet.contains(filteredTaskLog.getBuildId()) && StringUtils.isNotEmpty(filteredTaskLog.getBuildId()))
                            {
                                logger.info("current build id: {}", filteredTaskLog.getBuildId());
                                stopFlag = true;
                                buildIdSet.add(filteredTaskLog.getBuildId());
                            }
                            else
                            {
                                stopFlag = false;
                            }
                            asyncResultList.add(pipelineTaskService.handleStopTask(projectId, pipelineId, taskId, filteredTaskLog, userName, stopFlag, streamName));
                        }
                );
                asyncResultList.forEach(asyncResult ->
                {
                    try
                    {
                        asyncResult.get();
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        logger.error("stop task fail!");
                    }
                }
                );

            }
            logger.info("stop pipeline task and update task status success! task id:{}", taskId);
        }
        return true;
    }


}
