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

package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.TaskLogOverviewDao
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel
import com.tencent.bk.codecc.apiquery.service.TaskLogOverviewService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskLogOverviewServiceImpl @Autowired constructor(
        private val taskLogOverviewDao: TaskLogOverviewDao
) : TaskLogOverviewService {

    /**
     * 统计任务分析次数
     */
    override fun statTaskAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?, endTime: Long?): Int {
        return Math.toIntExact(taskLogOverviewDao.queryTaskAnalyzeCount(taskIds, status, startTime, endTime))
    }

    /**
     * 按任务ID分组统计时间范围内的分析次数
     *
     * @param taskIds   任务ID集合（必填）
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return list
     */
    override fun queryAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?,
                                   endTime: Long?): List<TaskLogOverviewModel> {
        return taskLogOverviewDao.statAnalyzeCountByTaskIds(taskIds, status, startTime, endTime)
    }

    /**
     * 按任务ID分组统计时间范围内的分析次数
     *
     * @param taskIds   任务ID集合
     * @param buildNum  最小分析次数
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return map taskId, count
     */
    override fun statAnalyzeCountByTaskIds(taskIds: Collection<Long>, buildNum: Int?, status: Int?, startTime: Long?,
                                           endTime: Long?): Map<Long, Int> {
        val analyzeCountStat = queryAnalyzeCount(taskIds, status, startTime, endTime)

        return if (analyzeCountStat.isNullOrEmpty()) {
            mapOf()
        } else {
            analyzeCountStat.filter { it.buildCount > buildNum ?: 0 }.associate { it.taskId to it.buildCount }
        }
    }

    /**
     * 按任务ID获取时间范围内的最新分析状态
     *
     * @param taskIds   任务ID集合（必填）
     * @param status    分析状态
     * @return list
     */
    override fun findLatestAnalyzeStatus(taskIds: Collection<Long>, status: Int?): List<TaskLogOverviewModel> {
        return taskLogOverviewDao.findLatestAnalyzeStatus(taskIds, status)
    }

    /**
     * 按任务ID查询时间范围内最新build id
     */
    override fun findBuildIdsByStartTime(taskIds: Collection<Long>, status: Int?, startTime: Long?,
                                         endTime: Long?): List<TaskLogOverviewModel> {
        return taskLogOverviewDao.findBuildIdsByStartTime(taskIds, status, startTime, endTime)
    }

}
