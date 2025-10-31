/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.auth.api.AuthUserAndDeptApi
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.event.pojo.measure.UserOperateCounterData
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.ProjectBuildSummaryDao
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import com.tencent.devops.metrics.service.CacheProjectInfoService
import com.tencent.devops.metrics.service.ProjectBuildSummaryService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ProjectBuildSummaryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectBuildSummaryDao: ProjectBuildSummaryDao,
    private val redisOperation: RedisOperation,
    private val cacheProjectInfoService: CacheProjectInfoService,
    private val authUserAndDeptApi: AuthUserAndDeptApi
) : ProjectBuildSummaryService {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectBuildSummaryServiceImpl::class.java)
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
            val projectVO = cacheProjectInfoService.getProject(projectId)
            if (projectVO?.enabled == false) {
                logger.info("Project [${projectVO.englishName}] has disabled, skip build count")
                return
            }
            projectBuildSummaryDao.saveBuildCount(
                dslContext = dslContext,
                projectId = projectId,
                productId = projectVO?.productId ?: 0,
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
            logger.info("save Project User:$projectId|$userId|$theDate")
            val projectVO = cacheProjectInfoService.getProject(projectId)
            if (projectVO?.enabled == false) {
                logger.info("Project [${projectVO.englishName}] has disabled, skip user count")
                return
            }
            if (authUserAndDeptApi.checkUserDeparted(userId)) {
                logger.debug("This user does not need to be save, because he has departed|$userId")
                return
            }
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
                        productId = projectVO?.productId ?: 0,
                        theDate = theDate
                    )
                }
            }
        }
    }

    override fun saveProjectUserOperateMetrics(
        userOperateCounterData: UserOperateCounterData
    ) {
        userOperateCounterData.getUserOperationCountMap().forEach { (projectUserOperateMetricsDataKey, operateCount) ->
            val projectUserOperateMetricsData = ProjectUserOperateMetricsData.build(
                projectUserOperateMetricsKey = projectUserOperateMetricsDataKey
            )
            JooqUtils.retryWhenDeadLock {
                try {
                    projectBuildSummaryDao.saveUserOperateCount(
                        dslContext = dslContext,
                        projectUserOperateMetricsData = projectUserOperateMetricsData,
                        operateCount = operateCount
                    )
                } catch (e: DuplicateKeyException) {
                    if (logger.isDebugEnabled) {
                        logger.debug(
                            "save project user operate metrics duplicate {} |{}",
                            projectUserOperateMetricsDataKey, operateCount
                        )
                    }
                    projectBuildSummaryDao.updateUserOperateCount(
                        dslContext = dslContext,
                        projectUserOperateMetricsData = projectUserOperateMetricsData,
                        operateCount = operateCount
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
