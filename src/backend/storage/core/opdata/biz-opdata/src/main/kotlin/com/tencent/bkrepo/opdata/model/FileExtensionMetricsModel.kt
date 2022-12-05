/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.model

import com.tencent.bkrepo.opdata.constant.OPDATA_FILE_EXTENSION_METRICS
import com.tencent.bkrepo.opdata.pojo.enums.StatMetrics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.aggregation.Aggregation.project
import org.springframework.data.mongodb.core.aggregation.Aggregation.sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

@Service
class FileExtensionMetricsModel @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    /**
     * 获取总体的文件后缀名的统计信息
     */
    fun getFileExtensionMetrics(metrics: StatMetrics): List<HashMap<String, Any>> {
        return aggregateQuery(Criteria(), metrics)
    }

    /**
     * 获取项目的文件后缀名的统计信息
     */
    fun getProjFileExtensionMetrics(projectId: String, metrics: StatMetrics): List<HashMap<String, Any>> {
        val criteria = where(TFileExtensionMetrics::projectId).isEqualTo(projectId)
        return aggregateQuery(criteria, metrics)
    }

    /**
     * 获取仓库的文件后缀名的统计信息
     */
    fun getRepoFileExtensionMetrics(
        projectId: String,
        repoName: String,
        metrics: StatMetrics
    ): List<HashMap<String, Any>> {
        val criteria = where(TFileExtensionMetrics::projectId).isEqualTo(projectId)
            .and(TFileExtensionMetrics::repoName).isEqualTo(repoName)
        return aggregateQuery(criteria, metrics)
    }

    @Suppress("UNCHECKED_CAST")
    private fun aggregateQuery(criteria: Criteria, metrics: StatMetrics): List<HashMap<String, Any>> {
        val field = metrics.name.toLowerCase()
        val aggregate = newAggregation(
            match(criteria),
            group(TFileExtensionMetrics::extension.name).sum(field).`as`(field),
            sort(Sort.Direction.DESC, field),
            project().andInclude(field).and(ID).`as`(TFileExtensionMetrics::extension.name).andExclude(ID)
        )

        val aggregateResult = mongoTemplate.aggregate(aggregate, OPDATA_FILE_EXTENSION_METRICS, HashMap::class.java)
        return aggregateResult.mappedResults as? List<HashMap<String, Any>> ?: listOf()
    }

    companion object {
        private const val ID = "_id"
    }
}
