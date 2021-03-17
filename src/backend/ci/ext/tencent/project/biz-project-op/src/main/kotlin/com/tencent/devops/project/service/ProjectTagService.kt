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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.project.api.op.pojo.OpProjectTagUpdateDTO
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectTagDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectTagService @Autowired constructor(
    val dslContext: DSLContext,
    val projectTagDao: ProjectTagDao,
    val redisOperation: RedisOperation,
    val projectDao: ProjectDao
) {

    private val executorService = Executors.newSingleThreadExecutor()

    fun updateTagByProject(
        opProjectTagUpdateDTO: OpProjectTagUpdateDTO
    ): Result<Boolean> {
        logger.info("updateTagByProject: $opProjectTagUpdateDTO")
        checkProject(opProjectTagUpdateDTO.projectCodeList)
        projectTagDao.updateProjectTags(
            dslContext = dslContext,
            projectIds = opProjectTagUpdateDTO.projectCodeList!!,
            consulTag = opProjectTagUpdateDTO.consulTags
        )
        executorService.submit {
            ProjectTagRefresh(
                consulTag = opProjectTagUpdateDTO.consulTags,
                redisOperation = redisOperation,
                projectCodeIds = opProjectTagUpdateDTO.projectCodeList!!
            )
        }
        return Result(true)
    }

    fun updateTagByOrg(
        opProjectTagUpdateDTO: OpProjectTagUpdateDTO
    ): Result<Boolean> {
        logger.info("updateTagByOrg: $opProjectTagUpdateDTO")
        checkOrg(opProjectTagUpdateDTO)
        projectTagDao.updateOrgTags(
            dslContext = dslContext,
            consulTag = opProjectTagUpdateDTO.consulTags,
            bgId = opProjectTagUpdateDTO.bgId,
            centerId = opProjectTagUpdateDTO.centerId,
            deptId = opProjectTagUpdateDTO.deptId
        )

        val projectCodes = projectDao.listByGroupId(
            dslContext = dslContext,
            bgId = opProjectTagUpdateDTO.bgId,
            centerId = opProjectTagUpdateDTO.centerId,
            deptId = opProjectTagUpdateDTO.deptId
        ).map { it.englishName }

        executorService.execute {
            ProjectTagRefresh(
                consulTag = opProjectTagUpdateDTO.consulTags,
                redisOperation = redisOperation,
                projectCodeIds = projectCodes
            )
        }
        return Result(true)
    }

    private fun checkProject(projectIds: List<String>?) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw ParamBlankException("Invalid projectIds")
        }
    }

    private fun checkOrg(opProjectTagUpdateDTO: OpProjectTagUpdateDTO) {
        if (opProjectTagUpdateDTO.bgId == null
            && opProjectTagUpdateDTO.deptId == null
            && opProjectTagUpdateDTO.centerId == null) {
            throw ParamBlankException("Invalid project org")
        }
    }

    class ProjectTagRefresh(
        val consulTag: String,
        val projectCodeIds: List<String>,
        val redisOperation: RedisOperation
    ) : Runnable {
        override fun run() {
            val watcher = Watcher("ProjectTagRefresh $consulTag")
            logger.info("ProjectTagRefresh start $consulTag $projectCodeIds")
            projectCodeIds.forEach { projectCode ->
                redisOperation.hset(TAG_REDIS_KEY, projectCode, consulTag)
            }
            logger.info("ProjectTagRefresh success. $consulTag ${projectCodeIds.size}")
            LogUtils.printCostTimeWE(watcher)
        }
    }

    companion object {
        const val TAG_REDIS_KEY = "project:setting:tag:v2"
        val logger = LoggerFactory.getLogger(ProjectTagService::class.java)
    }
}
