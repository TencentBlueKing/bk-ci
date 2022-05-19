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

package com.tencent.bkrepo.scanner.dao

import com.mongodb.client.result.UpdateResult
import com.tencent.bkrepo.scanner.model.TScanner
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScannerDao : ScannerSimpleMongoDao<TScanner>() {
    fun existsByName(name: String): Boolean {
        val query = buildQuery(name)
        return exists(query)
    }

    fun findByName(name: String): TScanner? {
        val query = buildQuery(name)
        return findOne(query)
    }

    fun deleteByName(name: String, deleted: LocalDateTime = LocalDateTime.now()): UpdateResult {
        val query = buildQuery(name)
        val update = Update.update(TScanner::deleted.name, deleted)
        return updateFirst(query, update)
    }

    fun list(includeDeleted: Boolean = false): List<TScanner> {
        val criteria = Criteria()
        if (!includeDeleted) {
            criteria.and(TScanner::deleted.name).isEqualTo(null)
        }
        return find(Query(criteria))
    }

    private fun buildQuery(name: String, includeDeleted: Boolean = false): Query {
        val criteria = TScanner::name.isEqualTo(name)
        if (!includeDeleted) {
            criteria.and(TScanner::deleted.name).isEqualTo(null)
        }
        return Query(criteria)
    }
}
