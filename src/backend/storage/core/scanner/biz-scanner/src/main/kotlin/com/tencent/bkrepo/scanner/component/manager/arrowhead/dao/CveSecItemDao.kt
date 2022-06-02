/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.component.manager.arrowhead.dao

import com.tencent.bkrepo.scanner.pojo.request.ArrowheadLoadResultArguments
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TCveSecItem
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TCveSecItemData
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Repository

@Repository
class CveSecItemDao : ResultItemDao<TCveSecItem>() {
    override fun customizePageBy(criteria: Criteria, arguments: ArrowheadLoadResultArguments): Criteria {
        if (arguments.vulnerabilityLevels.isNotEmpty()) {
            criteria.and(dataKey(TCveSecItemData::cvssRank.name)).inValues(arguments.vulnerabilityLevels)
        }
        if (arguments.vulIds.isNotEmpty()) {
            criteria.andOperator(buildVulIdCriteria(arguments.vulIds))
        }
        return criteria
    }

    private fun buildVulIdCriteria(vulIds: List<String>): Criteria {
        val cveIds = HashSet<String>()
        val cnnvdIds = HashSet<String>()
        val cnvdIds = HashSet<String>()
        val pocIds = HashSet<String>()

        vulIds.forEach { vulId ->
            val prefix = vulId.substring(0, vulId.indexOf('-')).toLowerCase()
            when(prefix) {
                "cve" -> cveIds.add(vulId)
                "cnnvd" -> cnnvdIds.add(vulId)
                "cnvd" -> cnvdIds.add(vulId)
                else -> pocIds.add(vulId)
            }
        }

        val criteria = Criteria()
        orVulIdsIn(criteria, dataKey(TCveSecItemData::cveId.name), cveIds)
        orVulIdsIn(criteria, dataKey(TCveSecItemData::cnnvdId.name), cnnvdIds)
        orVulIdsIn(criteria, dataKey(TCveSecItemData::cnvdId.name), cnvdIds)
        orVulIdsIn(criteria, dataKey(TCveSecItemData::pocId.name), pocIds)

        return criteria
    }

    private fun orVulIdsIn(criteria: Criteria, key: String, vulIds: Set<String>) {
        if (vulIds.isNotEmpty()) {
            criteria.orOperator(Criteria.where(key).inValues(vulIds))
        }
    }

    private fun dataKey(name: String) = "${TCveSecItem::data.name}.$name"
}
