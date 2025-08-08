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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.control.lock.PipelineVersionLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
@Suppress("LongParameterList", "ReturnCount", "ComplexMethod")
class PipelineRepositoryVersionService(
    private val dslContext: DSLContext,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
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
            val pipelineVersionInfo = pipelineResourceVersionDao.getPipelineVersionSimple(
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
            pipelineResourceVersionDao.updatePipelineVersionReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                versions = listOf(resourceVersion),
                referCount = referCount,
                referFlag = true
            )
        }
    }

    fun deletePipelineVersion(projectId: String, pipelineId: String, version: Int) {
        // 判断该流水线版本是否还有关联的构建记录，没有记录才能删除
        val pipelineVersionLock = PipelineVersionLock(redisOperation, pipelineId, version)
        try {
            pipelineVersionLock.lock()
            // #8161 软删除数据，前端无法查询到该版本
            pipelineResourceVersionDao.deleteByVersion(dslContext, projectId, pipelineId, version)
        } finally {
            pipelineVersionLock.unlock()
        }
    }

    fun getPipelineVersionWithInfo(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        version: Int,
        includeDraft: Boolean? = true
    ): PipelineVersionWithInfo? {
        if (pipelineInfo == null) {
            return null
        }
        val resource = pipelineResourceVersionDao.getVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = includeDraft
        ) ?: pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return null
        return PipelineVersionWithInfo(
            createTime = pipelineInfo.createTime,
            updateTime = resource.updateTime?.timestampmilli(),
            creator = pipelineInfo.creator,
            canElementSkip = pipelineInfo.canElementSkip,
            canManualStartup = pipelineInfo.canManualStartup,
            channelCode = pipelineInfo.channelCode,
            id = pipelineInfo.id,
            lastModifyUser = pipelineInfo.lastModifyUser,
            pipelineDesc = pipelineInfo.pipelineDesc,
            pipelineId = pipelineInfo.pipelineId,
            pipelineName = pipelineInfo.pipelineName,
            projectId = pipelineInfo.projectId,
            taskCount = pipelineInfo.taskCount,
            templateId = pipelineInfo.templateId,
            version = resource.version,
            versionName = resource.versionName ?: "",
            versionNum = resource.versionNum,
            pipelineVersion = resource.pipelineVersion,
            triggerVersion = resource.triggerVersion,
            settingVersion = resource.settingVersion,
            status = resource.status,
            debugBuildId = resource.debugBuildId,
            baseVersion = resource.baseVersion
        )
    }

    fun getPipelineVersionSimple(
        projectId: String,
        pipelineId: String,
        version: Int,
        archiveFlag: Boolean? = false
    ): PipelineVersionSimple? {
        return pipelineResourceVersionDao.getPipelineVersionSimple(
            dslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT),
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun listPipelineReleaseVersion(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        excludeVersion: Int?,
        versionName: String?,
        creator: String?,
        description: String?,
        buildOnly: Boolean? = false,
        archiveFlag: Boolean? = false
    ): Pair<Int, MutableList<PipelineVersionSimple>> {
        if (pipelineInfo == null) {
            return Pair(0, mutableListOf())
        }
        val finalDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        // 计算包括草稿在内的总数
        var count = pipelineResourceVersionDao.count(
            dslContext = finalDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            includeDraft = false,
            versionName = versionName,
            creator = creator,
            description = description,
            buildOnly = buildOnly
        )
        val result = pipelineResourceVersionDao.listPipelineVersion(
            dslContext = finalDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineInfo = pipelineInfo,
            creator = creator,
            description = description,
            versionName = versionName,
            includeDraft = false,
            excludeVersion = excludeVersion,
            offset = offset,
            limit = limit,
            buildOnly = buildOnly
        ).toMutableList()

        // #8161 当过滤草稿时查到空结果是正常的，只在不过滤草稿时兼容老数据的版本表无记录
        val noSearch = versionName.isNullOrBlank() && creator.isNullOrBlank() && description.isNullOrBlank()
        if (result.isEmpty() && pipelineInfo.latestVersionStatus?.isNotReleased() != true && noSearch) {
            pipelineResourceDao.getReleaseVersionResource(
                finalDslContext, projectId, pipelineId
            )?.let { record ->
                count = 1
                result.add(
                    PipelineVersionSimple(
                        pipelineId = record.pipelineId,
                        creator = record.creator,
                        createTime = record.createTime.timestampmilli(),
                        updater = record.updater,
                        updateTime = record.updateTime?.timestampmilli(),
                        version = record.version,
                        versionName = record.versionName ?: PipelineVersionUtils.getVersionName(
                            versionNum = record.version,
                            pipelineVersion = record.versionNum ?: record.version,
                            triggerVersion = 0,
                            settingVersion = 0
                        ) ?: "",
                        yamlVersion = record.yamlVersion,
                        referFlag = record.referFlag,
                        referCount = record.referCount,
                        versionNum = record.versionNum ?: record.version,
                        pipelineVersion = record.pipelineVersion,
                        triggerVersion = record.triggerVersion,
                        settingVersion = record.settingVersion,
                        status = record.status,
                        debugBuildId = record.debugBuildId,
                        baseVersion = record.baseVersion,
                        description = record.description
                    )
                )
            }
        }
        return count to result
    }

    fun listPipelineVersionWithInfo(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        includeDraft: Boolean?,
        excludeVersion: Int?,
        versionName: String?,
        creator: String?,
        description: String?
    ): Pair<Int, MutableList<PipelineVersionWithInfo>> {
        if (pipelineInfo == null) {
            return Pair(0, mutableListOf())
        }

        var count = pipelineResourceVersionDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            includeDraft = includeDraft,
            versionName = versionName,
            creator = creator,
            description = description
        )
        val result = mutableListOf<PipelineVersionSimple>()
        result.addAll(
            pipelineResourceVersionDao.listPipelineVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineInfo = pipelineInfo,
                creator = creator,
                description = description,
                versionName = versionName,
                includeDraft = includeDraft,
                excludeVersion = excludeVersion,
                offset = offset,
                limit = limit
            )
        )
        // #8161 当过滤草稿时查到空结果是正常的，只在不过滤草稿时兼容老数据的版本表无记录
        if (result.isEmpty() && pipelineInfo.latestVersionStatus?.isNotReleased() != true) {
            pipelineResourceDao.getReleaseVersionResource(
                dslContext, projectId, pipelineId
            )?.let { record ->
                count = 1
                result.add(
                    PipelineVersionSimple(
                        pipelineId = record.pipelineId,
                        creator = record.creator,
                        updater = record.updater,
                        createTime = record.createTime.timestampmilli(),
                        updateTime = record.updateTime?.timestampmilli(),
                        version = record.version,
                        versionName = record.versionName ?: PipelineVersionUtils.getVersionName(
                            versionNum = record.version,
                            pipelineVersion = record.versionNum ?: record.version,
                            triggerVersion = 0,
                            settingVersion = 0
                        ) ?: "",
                        yamlVersion = record.yamlVersion,
                        referFlag = record.referFlag,
                        referCount = record.referCount,
                        versionNum = record.versionNum ?: record.version,
                        pipelineVersion = record.pipelineVersion,
                        triggerVersion = record.triggerVersion,
                        settingVersion = record.settingVersion,
                        status = record.status,
                        debugBuildId = record.debugBuildId,
                        baseVersion = record.baseVersion,
                        description = record.description
                    )
                )
            }
        }
        val list = mutableListOf<PipelineVersionWithInfo>()

        result.forEach {
            val baseVersion = it.baseVersion
            list.add(
                PipelineVersionWithInfo(
                    createTime = it.createTime,
                    updateTime = it.updateTime,
                    creator = it.creator,
                    canElementSkip = pipelineInfo.canElementSkip,
                    canManualStartup = pipelineInfo.canManualStartup,
                    channelCode = pipelineInfo.channelCode,
                    id = pipelineInfo.id,
                    lastModifyUser = pipelineInfo.lastModifyUser,
                    pipelineDesc = pipelineInfo.pipelineDesc,
                    pipelineId = pipelineInfo.pipelineId,
                    pipelineName = pipelineInfo.pipelineName,
                    projectId = pipelineInfo.projectId,
                    taskCount = pipelineInfo.taskCount,
                    templateId = pipelineInfo.templateId,
                    version = it.version,
                    versionName = it.versionName,
                    versionNum = it.versionNum,
                    pipelineVersion = it.pipelineVersion,
                    triggerVersion = it.triggerVersion,
                    settingVersion = it.settingVersion,
                    status = it.status,
                    debugBuildId = it.debugBuildId,
                    baseVersion = baseVersion,
                    // 草稿单独获取一下版本名称给前端展示
                    baseVersionName = if (it.status == VersionStatus.COMMITTING && baseVersion != null) {
                        pipelineResourceVersionDao.getVersionResource(
                            dslContext, projectId, pipelineId, baseVersion
                        )?.versionName
                    } else null,
                    description = it.description
                )
            )
        }
        return count to list
    }

    fun getVersionCreatorInPage(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Pair<Int, List<String>> {
        if (pipelineInfo == null) {
            return Pair(0, emptyList())
        }

        val count = pipelineResourceVersionDao.countVersionCreator(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val result = pipelineResourceVersionDao.getVersionCreatorInPage(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        return count to result
    }

    fun saveDebugBuildInfo(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        version: Int,
        buildId: String
    ): Boolean {
        return pipelineResourceVersionDao.updateDebugBuildId(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            debugBuildId = buildId
        )
    }

    fun asyncBatchUpdateReferFlag(
        projectChannelCode: String,
        routerTag: AuthSystemType? = null,
        projectId: String? = null,
        queryUnknownRelatedFlag: Boolean? = null
    ): Boolean {
        val executors = Executors.newFixedThreadPool(1)
        try {
            executors.submit {
                logger.info("begin asyncBatchUpdateReferFlag!!")
                var offset = 0
                val limit = PageUtil.DEFAULT_PAGE_SIZE
                do {
                    val projectConditionDTO = ProjectConditionDTO(
                        channelCode = projectChannelCode,
                        routerTag = routerTag
                    ).apply {
                        projectId?.let { projectCodes = listOf(it) }
                    }
                    val projectInfos = client.get(ServiceProjectResource::class).listProjectsByCondition(
                        projectConditionDTO = projectConditionDTO,
                        limit = limit,
                        offset = offset
                    ).data ?: break
                    projectInfos.forEach { projectInfo ->
                        val pipelineIds = pipelineInfoDao.listPipelineIdByProject(dslContext, projectInfo.englishName)
                        pipelineIds.forEach { pipelineId ->
                            updatePipelineReferFlag(
                                projectId = projectInfo.englishName,
                                pipelineId = pipelineId,
                                queryUnknownRelatedFlag = queryUnknownRelatedFlag
                            )
                        }
                    }
                    offset += limit
                } while (projectInfos.size == limit)
                logger.info("end asyncBatchUpdateReferFlag!!")
            }
        } finally {
            executors.shutdown()
        }
        return true
    }

    private fun updatePipelineReferFlag(
        projectId: String,
        pipelineId: String,
        queryUnknownRelatedFlag: Boolean? = null
    ) {
        var offset = 0
        val limit = PageUtil.DEFAULT_PAGE_SIZE
        val pipelineInfo = pipelineInfoDao.convert(
            pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId), null
        ) ?: return
        val lock = PipelineModelLock(redisOperation, pipelineId)
        try {
            lock.lock()
            do {
                // 查询流水线版本记录
                val pipelineVersionList = pipelineResourceVersionDao.listPipelineVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineInfo = pipelineInfo,
                    queryUnknownRelatedFlag = queryUnknownRelatedFlag,
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
                    pipelineResourceVersionDao.updatePipelineVersionReferInfo(
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
                if (unReferVersions.isNotEmpty()) {
                    pipelineResourceVersionDao.updatePipelineVersionReferInfo(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        versions = unReferVersions,
                        referCount = 0,
                        referFlag = false
                    )
                }
                offset += limit
            } while (pipelineVersionList.size == limit)
        } finally {
            lock.unlock()
        }
    }
}
