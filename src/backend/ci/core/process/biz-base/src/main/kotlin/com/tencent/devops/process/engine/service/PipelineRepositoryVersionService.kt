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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.PipelineVersionLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.utils.PipelineVersionUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList", "ReturnCount")
class PipelineRepositoryVersionService(
    private val dslContext: DSLContext,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val redisOperation: RedisOperation
) {

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
            val referFlag = pipelineVersionInfo?.referFlag ?: true
            val referCount = pipelineVersionInfo?.referCount?.let { self -> self + 1 }
            // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
                ?: pipelineBuildDao.countBuildNumByVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = resourceVersion
                )

            // 更新流水线版本关联构建记录信息
            pipelineResourceVersionDao.updatePipelineVersionReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = resourceVersion,
                referCount = referCount,
                referFlag = referFlag
            )
        }
    }

    fun deletePipelineVersion(projectId: String, pipelineId: String, version: Int) {
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
                // #8161 软删除数据，前端无法查询到该版本
                pipelineResourceVersionDao.deleteByVersion(transactionContext, projectId, pipelineId, version)
//                pipelineSettingVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
            }
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
        version: Int
    ): PipelineVersionSimple? {
        return pipelineResourceVersionDao.getPipelineVersionSimple(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun listPipelineVersion(
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
}
