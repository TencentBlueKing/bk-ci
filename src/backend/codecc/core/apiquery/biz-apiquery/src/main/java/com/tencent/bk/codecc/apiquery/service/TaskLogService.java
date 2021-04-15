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
package com.tencent.bk.codecc.apiquery.service;


import com.tencent.bk.codecc.apiquery.defect.model.TaskLogModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TaskLogService
{
    /**
     * 批量获取任务的工具最近分析成功的记录
     *
     * @param taskIds   任务ID集合
     * @param toolName  工具名
     * @return list
     */
    Map<Long, List<TaskLogModel>> batchTaskLogSuccessList(Set<Long> taskIds, String toolName);


    /**
     * 批量获取任务的工具最近分析成功的记录
     *
     * @param taskIds   任务ID集合
     * @param toolName  工具名
     * @return list
     */
    Map<Long, Integer> batchTaskLogCountList(Set<Long> taskIds, String toolName);


    /**
     * 按时间批量获取最近成功的记录
     *
     * @param taskIds   任务ID集合
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return list
     */
    Map<Long, List<TaskLogModel>> batchFindByTaskIdListAndTime(List<Long> taskIds, Long startTime, Long endTime);


    /**
     * 批量获取任务的工具最新一条分析记录
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    Map<Long, TaskLogModel> batchFindLastTaskLogByTool(List<Long> taskIds, String toolName);


    /**
     * 获取指定构建ID的分析记录
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @param buildId  构建ID
     * @return model
     */
    TaskLogModel getBuildTaskLog(long taskId, String toolName, String buildId);

}
