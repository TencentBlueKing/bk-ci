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

package com.tencent.bk.codecc.apiquery.defect.dao

import com.google.common.collect.Lists
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDefectStatModel
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class CheckerDefectStatDao @Autowired constructor(
        private val defectMongoTemplate: MongoTemplate
) {

    /**
     * 多条件分页查询规则告警数
     */
    fun findCheckerDefectStatPage(reqReq: TaskToolInfoReqVO, pageable: Pageable): Page<CheckerDefectStatModel> {
        val criteriaList: MutableList<Criteria> = Lists.newArrayList()
        val endTime = reqReq.endTime
        if (StringUtils.isNotEmpty(endTime)) {
            criteriaList.add(Criteria.where("stat_date").gte(DateTimeUtils.getTimeStampStart(endTime)).lte(
                    DateTimeUtils.getTimeStampEnd(endTime)))
        }
        val createFrom = reqReq.createFrom
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").`in`(createFrom))
        }
        val toolName = reqReq.toolName
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").`is`(toolName))
        }

        // 搜索条件 checker_set_name
        val searchString = reqReq.searchString
        if (StringUtils.isNotEmpty(searchString)) {
            val quickSearchCriteria = Lists.newArrayList<Criteria>()
            quickSearchCriteria.add(Criteria.where("checker_name").regex(searchString))
            criteriaList.add(Criteria().orOperator(*quickSearchCriteria.toTypedArray()))
        }

        val criteria = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(* criteriaList.toTypedArray())
        }
        val match = Aggregation.match(criteria)
        val totalCount = defectMongoTemplate.count(Query(criteria), "t_checker_defect_stat")

        // 分页排序
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val sort = Aggregation.sort(pageable.sort)
        val skip = Aggregation.skip((pageNumber * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val aggregation = Aggregation.newAggregation(match, sort, skip, limit)
        val queryResults =
                defectMongoTemplate.aggregate(aggregation, "t_checker_defect_stat", CheckerDefectStatModel::class.java)

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        // 页码加1返回
        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.mappedResults)
    }
}