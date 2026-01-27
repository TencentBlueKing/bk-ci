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

package com.tencent.devops.process.service.`var`

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.pojo.`var`.VarCountUpdateInfo
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 公共变量引用计数服务
 * 提供变量级别的引用计数更新机制
 * 
 * 注意：该类不提供锁保护，锁保护由外层（PublicVarReferInfoService）统一控制。
 */
@Service
class PublicVarReferCountService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferCountService::class.java)
    }

    /**
     * 批量新增引用并更新引用计数（无锁版本）
     * @param referInfos 引用信息列表
     */
    fun batchAddReferWithCount(referInfos: List<ResourcePublicVarReferPO>) {
        if (referInfos.isEmpty()) {
            return
        }

        // 按版本分组处理
        val groupedByVersion = referInfos.groupBy { it.version ?: -1 }

        groupedByVersion.forEach { (version, varReferInfosForVersion) ->
            // 按 (sourceProjectId, groupName, varName) 分组
            // 注意：需要按 sourceProjectId 分组，因为不同变量可能来自不同项目
            val groupedByVar = varReferInfosForVersion.groupBy {
                val actualSourceProjectId = it.sourceProjectId ?: it.projectId
                Triple(actualSourceProjectId, it.groupName, it.varName)
            }

            // 按固定顺序排序，保持一致的执行顺序，避免死锁
            val sortedVarGroups = groupedByVar.toList().sortedWith(
                compareBy<Pair<Triple<String, String, String>, List<ResourcePublicVarReferPO>>> { it.first.first }
                    .thenBy { it.first.second }
                    .thenBy { it.first.third }
            )

            // 优化：使用单个事务批量处理所有变量组，减少事务数量
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                sortedVarGroups.forEach nextVarGroup@{ (key, varReferInfos) ->
                    val (sourceProjectId, groupName, varName) = key
                    // 验证引用记录列表不为空
                    if (varReferInfos.isEmpty()) {
                        logger.warn("Empty varReferInfos list for groupName:$groupName, varName:$varName, skip")
                        return@nextVarGroup
                    }
                    logger.info(
                        "Processing variable reference addition: " +
                            "sourceProjectId=$sourceProjectId, groupName=$groupName, varName=$varName, " +
                            "version=$version, count=${varReferInfos.size}"
                    )

                    // 1. 批量插入引用记录（使用当前 projectId，引用记录存储在当前项目）
                    publicVarReferInfoDao.batchSave(
                        dslContext = context,
                        pipelinePublicVarReferPOs = varReferInfos
                    )

                    // 2. 增量更新引用计数（使用 sourceProjectId，计数存储在变量组所在项目）
                    incrementReferCount(
                        context = context,
                        projectId = sourceProjectId,
                        groupName = groupName,
                        varName = varName,
                        version = version,
                        countChange = varReferInfos.size
                    )
                }
            }
        }
    }

    /**
     * 增加引用计数（增量更新）
     * 
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param varName 变量名称
     * @param version 版本号（动态版本为-1）
     * @param countChange 增加的数量
     */
    fun incrementReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int,
        countChange: Int
    ) {
        // 检查是否存在该版本的概要信息
        val existingSummary = publicVarVersionSummaryDao.getByVarNameAndVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            varName = varName,
            version = version
        )

        if (existingSummary != null) {
            // 使用增量更新
            publicVarVersionSummaryDao.incrementReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                countChange = countChange,
                modifier = SYSTEM
            )
        } else {
            createAndSaveNewVersionSummary(
                context = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = countChange
            )
        }
    }

    /**
     * 创建并保存新的版本概要记录（生成 ID、写入 DB）
     */
    private fun createAndSaveNewVersionSummary(
        context: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int,
        referCount: Int
    ) {
        val currentTime = LocalDateTime.now()
        val summaryPO = PublicVarVersionSummaryPO(
            id = client.get(ServiceAllocIdResource::class)
                .generateSegmentId("T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY").data ?: 0,
            projectId = projectId,
            groupName = groupName,
            varName = varName,
            version = version,
            referCount = referCount,
            creator = SYSTEM,
            modifier = SYSTEM,
            createTime = currentTime,
            updateTime = currentTime
        )
        publicVarVersionSummaryDao.save(dslContext = context, po = summaryPO)
    }

    /**
     * 重新计算引用计数（无锁版本）
     * 
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarReferInfoService）已经提供了锁保护。
     * 
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param varName 变量名称
     * @param version 版本号（null表示重新计算动态版本，即-1）
     */
    fun recalculateReferCount(
        projectId: String,
        groupName: String,
        varName: String,
        version: Int? = null
    ) {
        val targetVersion = version ?: -1 // 如果为null，则重新计算动态版本
        // 在事务中执行，注意：外层已经提供了锁保护
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 重新计算指定版本的引用计数
            recalculateSingleVersionReferCount(
                context = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = targetVersion
            )
        }
    }

    /**
     * 重新计算单个版本的引用计数
     */
    private fun recalculateSingleVersionReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int
    ) {
        // 统计该版本的实际引用数量
        val actualReferCount = publicVarReferInfoDao.countDistinctReferIdsByVar(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version,
            varName = varName
        )

        // 检查是否存在该版本的概要信息
        val existingSummary = publicVarVersionSummaryDao.getByVarNameAndVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            varName = varName,
            version = version
        )

        if (existingSummary != null) {
            // 更新现有记录的引用计数
            publicVarVersionSummaryDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = actualReferCount,
                modifier = SYSTEM
            )
        } else if (actualReferCount > 0) {
            createAndSaveNewVersionSummary(
                context = context,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = actualReferCount
            )
        }
    }

    /**
     * 更新变量维度的引用计数（无锁版本）
     * 
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarReferInfoService）已经提供了锁保护。
     * 
     * @param referRecordsToAdd 需要新增的引用记录列表
     * @param varsNeedRecalculate 需要重新计算计数的变量信息集合
     */
    fun updateVarReferCounts(
        referRecordsToAdd: List<ResourcePublicVarReferPO>,
        varsNeedRecalculate: Set<VarCountUpdateInfo>
    ) {
        // 处理新增引用
        if (referRecordsToAdd.isNotEmpty()) {
            batchAddReferWithCount(referRecordsToAdd)
        }

        // 处理需要重新计算计数的变量
        if (varsNeedRecalculate.isNotEmpty()) {
            // 按固定顺序排序，保持一致的执行顺序，避免死锁
            val sortedVars = varsNeedRecalculate.sortedWith(
                compareBy<VarCountUpdateInfo> { it.projectId }
                    .thenBy { it.groupName }
                    .thenBy { it.varName }
                    .thenBy { it.version }
            )

            // 按固定顺序处理，避免死锁
            sortedVars.forEach { varInfo ->
                try {
                    recalculateReferCount(
                        projectId = varInfo.projectId,
                        groupName = varInfo.groupName,
                        varName = varInfo.varName,
                        version = varInfo.version
                    )
                } catch (e: Throwable) {
                    // 单个变量重算失败不影响其他变量
                    logger.warn(
                        "Failed to recalculate refer count for var: ${varInfo.varName}, " +
                            "group: ${varInfo.groupName}, project: ${varInfo.projectId}, " +
                            "version: ${varInfo.version}",
                        e
                    )
                }
            }
        }
    }
}
