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
 */

package com.tencent.devops.misc.service.project

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.misc.dao.project.ProjectDataMigrateHistoryDao
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.misc.utils.MiscUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectDataMigrateHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val projectDataMigrateHistoryDao: ProjectDataMigrateHistoryDao
) {

    /**
     * 新增迁移成功记录
     * @param userId 用户ID
     * @param projectDataMigrateHistory 项目数据迁移成功记录
     * @return 布尔值
     */
    fun add(userId: String, projectDataMigrateHistory: ProjectDataMigrateHistory): Boolean {
        projectDataMigrateHistoryDao.add(dslContext, userId, projectDataMigrateHistory)
        return true
    }

    /**
     * 判断项目或者流水线的数据是否能迁移
     * @param queryParam 查询参数
     * @return 布尔值
     */
    fun isDataCanMigrate(queryParam: ProjectDataMigrateHistoryQueryParam): Boolean {
        val projectId = queryParam.projectId
        val pipelineId = queryParam.pipelineId
        // 判读项目或者流水线的数据是否能迁移
        val migratingFlag = if (pipelineId.isNullOrBlank()) {
            redisOperation.isMember(
                key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
                item = projectId
            )
        } else {
            redisOperation.isMember(
                key = BkApiUtil.getMigratingPipelinesRedisKey(SystemModuleEnum.PROCESS.name),
                item = pipelineId
            )
        }
        // 判断项目或者流水线的数据是否能迁移
        return !migratingFlag && projectDataMigrateHistoryDao.getLatestProjectDataMigrateHistory(
            dslContext = dslContext,
            queryParam = queryParam
        ) == null
    }

    fun isDataCanDelete(queryParam: ProjectDataMigrateHistoryQueryParam): Boolean {
        return projectDataMigrateHistoryDao.getLatestProjectDataMigrateHistory(dslContext, queryParam) === null
    }
}
