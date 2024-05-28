/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
 *
 */

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.ProjectBuildSummaryDao
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import com.tencent.devops.metrics.service.CacheProjectInfoService
import com.tencent.devops.metrics.service.ProjectBuildSummaryService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProjectBuildSummaryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectBuildSummaryDao: ProjectBuildSummaryDao,
    private val redisOperation: RedisOperation,
    private val cacheProjectInfoService: CacheProjectInfoService
) : ProjectBuildSummaryService {

    companion object {
        private fun projectBuildKey(key: String) = "ProjectBuild:$key"
    }

    override fun saveProjectBuildCount(
        projectId: String,
        trigger: String?
    ) {
        if (trigger.isNullOrEmpty()) {
            return
        }
        val lock = RedisLock(redisOperation, projectBuildKey(projectId), 120)
        lock.use {
            lock.lock()
            val productId = cacheProjectInfoService.getProjectId(projectId)
            projectBuildSummaryDao.saveBuildCount(
                dslContext = dslContext,
                projectId = projectId,
                productId = productId,
                trigger = trigger
            )
        }
    }

    override fun saveProjectUser(
        projectId: String,
        userId: String,
        theDate: LocalDate
    ) {
        val lock = RedisLock(redisOperation, projectBuildKey(projectId), 120)
        lock.use {
            lock.lock()
            val productId = cacheProjectInfoService.getProjectId(projectId)
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                val insert = projectBuildSummaryDao.saveProjectUser(
                    dslContext = transactionContext,
                    projectId = projectId,
                    userId = userId
                )
                if (insert > 0) {
                    projectBuildSummaryDao.saveUserCount(
                        dslContext = dslContext,
                        projectId = projectId,
                        productId = productId,
                        theDate = theDate
                    )
                }
            }
        }
    }

    override fun getProjectActiveUserCount(
        baseQueryReq: BaseQueryReqVO
    ): ProjectUserCountV0? {
        return projectBuildSummaryDao.getProjectUserCount(
            dslContext = dslContext,
            baseQueryReq = baseQueryReq
        )
    }
}
