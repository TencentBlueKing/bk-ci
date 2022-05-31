/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.repository.model.TPackageDependents
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

/**
 * 包依赖关系数据访问层
 */
@Repository
class PackageDependentsDao : SimpleMongoDao<TPackageDependents>() {

    fun findByPackageKey(projectId: String, repoName: String, key: String): TPackageDependents? {
        if (key.isBlank()) {
            return null
        }
        return this.findOne(PackageQueryHelper.packageQuery(projectId, repoName, key))
    }

    fun addDependent(projectId: String, repoName: String, key: String, dependent: String): Long {
        if (key.isBlank() || dependent.isBlank()) {
            return 0
        }
        val query = PackageQueryHelper.packageQuery(projectId, repoName, key)
        val update = Update().addToSet(TPackageDependents::dependents.name, dependent)
        return this.upsert(query, update).modifiedCount
    }

    fun reduceDependent(projectId: String, repoName: String, key: String, dependent: String): Long {
        if (key.isBlank() || dependent.isBlank()) {
            return 0
        }
        val query = PackageQueryHelper.packageQuery(projectId, repoName, key)
        val update = Update().pull(TPackageDependents::dependents.name, dependent)
        return this.upsert(query, update).modifiedCount
    }
}
