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

package com.tencent.bk.codecc.apiquery.task.dao

import com.tencent.bk.codecc.apiquery.task.model.TaskStatisticModel
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

/**
 * 任务统计持久层
 */
@Repository
class TaskStatisticDao @Autowired constructor(private val taskMongoTemplate: MongoTemplate) {
    /**
     * 根据时间、来源、获取TaskStatisticModel集合
     */
    fun findTaskCountByCreateFromAndTime(dates: MutableList<String>, createFrom: MutableSet<String>): MutableList<TaskStatisticModel>? {
        val criteria = getTaskSearchCriteria(dates, createFrom)
        // 排序(按时间升序)
        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "date"))

        var group: GroupOperation? = null
        if (createFrom.size != 1) {
            // 分组统计
            group = Aggregation.group("date").first("date").`as`("date")
                    .sum("task_count").`as`("task_count")
                    .sum("active_count").`as`("active_count")
        }

        val aggregation = if (group == null) {
            // 指定查询字段
            val project = Aggregation.project("date", "task_count", "active_count")
            Aggregation.newAggregation(Aggregation.match(criteria), sort, project)
        } else {
            Aggregation.newAggregation(Aggregation.match(criteria), group, sort)
        }

        val aggregate = taskMongoTemplate.aggregate(aggregation, "t_task_statistic", TaskStatisticModel::class.java)
        return aggregate.mappedResults
    }

    fun findTaskAnalyzeCountByCreateFromAndTime(dates: MutableList<String>, createFrom: MutableSet<String>): MutableList<TaskStatisticModel> {
        val criteria = getTaskSearchCriteria(dates, createFrom)
        // 排序(按时间升序)
        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "date"))

        var group: GroupOperation? = null
        if (createFrom.size != 1) {
            // 分组统计
            group = Aggregation.group("date").first("date").`as`("date")
                    .sum("analyze_count").`as`("analyze_count")
        }
        // 指定查询字段
        val aggregation = if (group == null) {
            val project = Aggregation.project("date", "analyze_count")
            Aggregation.newAggregation(Aggregation.match(criteria), sort, project)
        } else {
            Aggregation.newAggregation(Aggregation.match(criteria), group, sort)
        }

        val aggregate = taskMongoTemplate.aggregate(aggregation, "t_task_statistic", TaskStatisticModel::class.java)
        return aggregate.mappedResults
    }

    /**
     * 任务数 任务活跃数 任务分析次数 查询条件
     */
    private fun getTaskSearchCriteria(dates: MutableList<String>, createFrom: MutableSet<String>): Criteria {
        val criteriaList = ArrayList<Criteria>()

        // 时间
        if (CollectionUtils.isNotEmpty(dates)) {
            criteriaList.add(Criteria.where("date").`in`(dates))
        }
        // 来源
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").`in`(createFrom))
        }
        val criteria = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(*criteriaList.toTypedArray())
        }
        return criteria
    }
}
