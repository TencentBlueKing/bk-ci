/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogModel;
import com.tencent.bk.codecc.apiquery.service.TaskLogService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务分析记录服务实现类
 *
 * @version V2.0
 * @date 2020/5/15
 */
@Service
public class TaskLogServiceImpl implements TaskLogService {

    private static Logger logger = LoggerFactory.getLogger(TaskLogServiceImpl.class);

    @Autowired
    private TaskLogDao taskLogDao;


    @Override
    public Map<Long, List<TaskLogModel>> batchTaskLogSuccessList(Set<Long> taskIds, String toolName) {
        List<TaskLogModel> lastTaskLogList = taskLogDao.findLastTaskLogList(taskIds, toolName);
        return lastTaskLogList.stream().collect(Collectors.groupingBy(TaskLogModel::getTaskId));
    }

    @Override
    public Map<Long, Integer> batchTaskLogCountList(Set<Long> taskIds, String toolName) {
        List<TaskLogModel> taskAnalyzeCount = taskLogDao.findTaskAnalyzeCount(taskIds, toolName);
        return taskAnalyzeCount.stream().collect(Collectors.toMap(TaskLogModel::getTaskId, TaskLogModel::getFlag));
    }

    @Override
    public Map<Long, List<TaskLogModel>> batchFindByTaskIdListAndTime(List<Long> taskIds, Long startTime,
            Long endTime) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Maps.newHashMap();
        }
        List<TaskLogModel> taskLogModels = taskLogDao.findLastTaskLogByTime(taskIds, startTime, endTime);
        return taskLogModels.stream().collect(Collectors.groupingBy(TaskLogModel::getTaskId));
    }


    /**
     * 批量获取任务的工具最新一条分析记录
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    @Override
    public Map<Long, TaskLogModel> batchFindLastTaskLogByTool(List<Long> taskIds, String toolName) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Maps.newHashMap();
        }
        List<TaskLogModel> taskLogModels = taskLogDao.batchFindLastTaskLogByTool(taskIds, toolName);
        return taskLogModels.stream().collect(Collectors.groupingBy(TaskLogModel::getTaskId)).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().iterator().next()));
    }


    /**
     * 获取指定构建ID的分析记录
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @param buildId  构建ID
     * @return model
     */
    @Override
    public TaskLogModel getBuildTaskLog(long taskId, String toolName, String buildId) {
        logger.info("begin getBuildTaskLog: {}, {}, {}", taskId, toolName, buildId);

        TaskLogModel taskLogModel;
        List<TaskLogModel.TaskUnit> taskUnits = Lists.newArrayList();
        taskLogModel = taskLogDao.findByBuildId(taskId, toolName, buildId);
        if (taskLogModel != null) {
            if (CollectionUtils.isNotEmpty(taskLogModel.getStepArray())) {
                for (TaskLogModel.TaskUnit taskEntityUnit : taskLogModel.getStepArray()) {
                    TaskLogModel.TaskUnit taskVOUnit = new TaskLogModel.TaskUnit();
                    BeanUtils.copyProperties(taskEntityUnit, taskVOUnit);
                    taskUnits.add(taskVOUnit);
                }
            }
        } else {
            taskLogModel = new TaskLogModel();
            taskLogModel.setTaskId(taskId);
            taskLogModel.setToolName(toolName);
            taskLogModel.setBuildId(buildId);
        }
        taskLogModel.setStepArray(taskUnits);

        logger.info("end getBuildTaskLog");
        return taskLogModel;
    }
}
