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

package com.tencent.bkrepo.repository.service.packages.impl

import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageDependentsDao
import com.tencent.bkrepo.repository.pojo.dependent.PackageDependentsRelation
import com.tencent.bkrepo.repository.service.packages.PackageDependentsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PackageDependentsServiceImpl(
    private val packageDao: PackageDao,
    private val packageDependentsDao: PackageDependentsDao
) : PackageDependentsService {

    override fun addDependents(request: PackageDependentsRelation) {
        with(request) {
            var addCount = 0L
            dependencies.forEach {
                if (packageDao.findByKey(projectId, repoName, it) != null) {
                    addCount += packageDependentsDao.addDependent(projectId, repoName, it, packageKey)
                }
            }
            logger.info("Create [$addCount] dependents for package [$projectId/$repoName/$packageKey]")
        }
    }

    override fun reduceDependents(request: PackageDependentsRelation) {
        with(request) {
            var reduceCount = 0L
            dependencies.forEach {
                if (packageDao.findByKey(projectId, repoName, it) != null) {
                    reduceCount += packageDependentsDao.reduceDependent(projectId, repoName, it, packageKey)
                }
            }
            logger.info("Delete [$reduceCount] dependents for package [$projectId/$repoName/$packageKey]")
        }
    }

    override fun findByPackageKey(projectId: String, repoName: String, packageKey: String): Set<String> {
        return packageDependentsDao.findByPackageKey(projectId, repoName, packageKey)?.dependents.orEmpty()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PackageDependentsServiceImpl::class.java)
    }
}
