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

import com.mongodb.client.result.DeleteResult
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.scanner.pojo.request.ArrowheadLoadResultArguments
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.ResultItem
import com.tencent.bkrepo.scanner.dao.ScannerSimpleMongoDao
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

abstract class ResultItemDao<T : ResultItem<*>> : ScannerSimpleMongoDao<T>() {

    fun deleteBy(credentialsKey: String?, sha256: String, scanner: String): DeleteResult {
        val criteria = buildCriteria(credentialsKey, sha256, scanner)
        return remove(Query(criteria))
    }

    fun pageBy(
        credentialsKey: String?,
        sha256: String,
        scanner: String,
        pageLimit: PageLimit,
        arguments: ArrowheadLoadResultArguments
    ): Page<T> {
        val pageable = PageRequest.of(pageLimit.pageNumber - 1, pageLimit.pageSize)
        val criteria = buildCriteria(credentialsKey, sha256, scanner)
        customizePageBy(criteria, arguments)
        val query = Query(criteria).with(pageable)
        val total = count(Query.of(query).limit(0).skip(0))
        val data = find(query)
        return Page(pageLimit.pageNumber, pageLimit.pageSize, total, data)
    }

    protected open fun customizePageBy(criteria: Criteria, arguments: ArrowheadLoadResultArguments): Criteria {
        return criteria
    }

    private fun buildCriteria(credentialsKey: String?, sha256: String, scanner: String): Criteria {
        return Criteria
            .where(ResultItem<*>::credentialsKey.name).isEqualTo(credentialsKey)
            .and(ResultItem<*>::sha256.name).isEqualTo(sha256)
            .and(ResultItem<*>::scanner.name).isEqualTo(scanner)
    }
}
