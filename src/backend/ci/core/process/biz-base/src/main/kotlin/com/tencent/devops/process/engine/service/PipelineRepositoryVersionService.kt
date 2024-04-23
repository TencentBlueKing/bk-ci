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
 */

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.control.lock.PipelineVersionLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class PipelineRepositoryVersionService(
    private val dslContext: DSLContext,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRepositoryVersionService::class.java)
    }

    fun addVerRef(projectId: String, pipelineId: String, resourceVersion: Int) {
        PipelineVersionLock(redisOperation, pipelineId, resourceVersion).use { versionLock ->
            versionLock.lock()
            // 查询流水线版本记录
            val pipelineVersionInfo = pipelineResVersionDao.getPipelineVersionSimple(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = resourceVersion
            )
            var referCount = pipelineVersionInfo?.referCount?.let { self -> self + 1 }
            // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
            if (referCount == null || referCount < 0) {
                referCount = pipelineBuildDao.countBuildNumByVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = resourceVersion
                )
            }

            // 更新流水线版本关联构建记录信息
            pipelineResVersionDao.updatePipelineVersionReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                versions = listOf(resourceVersion),
                referCount = referCount,
                referFlag = true
            )
        }
    }

    fun deletePipelineVer(projectId: String, pipelineId: String, version: Int) {
        // 判断该流水线版本是否还有关联的构建记录，没有记录才能删除
        val pipelineVersionLock = PipelineVersionLock(redisOperation, pipelineId, version)
        try {
            pipelineVersionLock.lock()
            val count = pipelineBuildDao.countBuildNumByVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
            if (count > 0) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CAN_NOT_DELETE_WHEN_HAVE_BUILD_RECORD
                )
            }
            dslContext.transaction { t ->
                val transactionContext = DSL.using(t)
                pipelineResVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
                pipelineSettingVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
            }
        } finally {
            pipelineVersionLock.unlock()
        }
    }

    fun listPipelineVersion(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Pair<Int, List<PipelineInfo>> {
        if (pipelineInfo == null) {
            return Pair(0, emptyList())
        }

        val count = pipelineResVersionDao.count(dslContext, projectId, pipelineId)
        val result = pipelineResVersionDao.listPipelineVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        val list = mutableListOf<PipelineInfo>()

        result.forEach {
            list.add(
                pipelineInfo.copy(
                    createTime = it.createTime,
                    creator = it.creator,
                    version = it.version,
                    versionName = it.versionName
                )
            )
        }
        return count to list
    }

    fun asyncBatchUpdateReferFlag(
        projectChannelCode: String
    ): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncBatchUpdateReferFlag!!")
            var offset = 0
            val limit = PageUtil.DEFAULT_PAGE_SIZE
            do {
                val projectInfos = client.get(ServiceProjectResource::class).listMigrateProjects(
                    migrateProjectConditionDTO = MigrateProjectConditionDTO(
                        channelCode = projectChannelCode
                    ),
                    limit = limit,
                    offset = offset
                ).data ?: break
                projectInfos.forEach { projectInfo ->
                    val projectId = projectInfo.englishName
                    val pipelineIds = pipelineInfoDao.listPipelineIdByProject(dslContext, projectId)
                    pipelineIds.forEach { pipelineId ->
                        updatePipelineReferFlag(projectId, pipelineId)
                    }
                }
                offset += limit
            } while (projectInfos.size == limit)
            logger.info("end asyncBatchUpdateReferFlag!!")
        }
        return true
    }

    private fun updatePipelineReferFlag(projectId: String, pipelineId: String) {
        var offset = 0
        val limit = PageUtil.DEFAULT_PAGE_SIZE
        val lock = PipelineModelLock(redisOperation, pipelineId)
        try {
            lock.lock()
            do {
                // 查询关联状态未知的版本
                val pipelineVersionList = pipelineResVersionDao.listPipelineVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    queryUnknownRelatedFlag = true,
                    offset = offset,
                    limit = limit
                )
                val versions = pipelineVersionList.map { it.version }.toSet()
                // 批量查询流水线版本号的构建记录
                val versionBuildNumMap = pipelineBuildDao.batchCountBuildNumByVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    versions = versions
                ).associateBy({ it.value1() }, { it.value2() })
                // 批量把流水线版本记录置为关联状态
                versionBuildNumMap.forEach { (version, buildNum) ->
                    pipelineResVersionDao.updatePipelineVersionReferInfo(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        versions = listOf(version),
                        referCount = buildNum,
                        referFlag = true
                    )
                }
                // 过滤出未关联的流水线版本号
                val unReferVersions =
                    versions.filter { versionBuildNumMap[it] == null || (versionBuildNumMap[it] ?: 0) < 1 }
                // 批量把流水线版本记录置为未关联状态
                pipelineResVersionDao.updatePipelineVersionReferInfo(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    versions = unReferVersions,
                    referCount = 0,
                    referFlag = false
                )
                offset += limit
            } while (pipelineVersionList.size == limit)
        } finally {
            lock.unlock()
        }
    }
}
