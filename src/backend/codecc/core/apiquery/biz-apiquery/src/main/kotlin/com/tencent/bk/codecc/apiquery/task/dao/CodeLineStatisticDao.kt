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

import com.tencent.bk.codecc.apiquery.defect.model.CodeLineStatisticModel
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

@Repository
class CodeLineStatisticDao @Autowired constructor(
    private val taskMongoTemplate: MongoTemplate
) {

    /**
     * 根据时间、来源 获取代码总量、每日分析代码总量趋势图数据
     */
    fun findByDateAndCreateFrom(dates: List<String>, createFrom: Set<String>): List<CodeLineStatisticModel> {
        // 筛选条件
        val criteriaList = mutableListOf<Criteria>()
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
        // 排序(按时间升序)
        val sort = Aggregation.sort(Sort.Direction.ASC, "date")
        // 分组
        var group: GroupOperation? = null
        // 来源参数是否多选
        if (createFrom.size != 1) {
            group = Aggregation.group("date").first("date").`as`("date")
                    .sum("sum_code").`as`("sum_code")
                    .sum("daily_code").`as`("daily_code")
                    .sum("daily_blank").`as`("daily_blank")
                    .sum("daily_comment").`as`("daily_comment")
        }
        val aggregation = if (group == null) {
            Aggregation.newAggregation(Aggregation.match(criteria), sort)
        } else {
            Aggregation.newAggregation(Aggregation.match(criteria), sort, group)
        }
        val queryResults =
                taskMongoTemplate.aggregate(aggregation, "t_code_line_statistic", CodeLineStatisticModel::class.java)
        return queryResults.mappedResults
    }
}