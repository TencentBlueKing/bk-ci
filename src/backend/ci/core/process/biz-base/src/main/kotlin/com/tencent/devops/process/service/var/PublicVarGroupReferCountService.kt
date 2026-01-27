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
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupVersionSummaryDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.VarGroupVersionChangeInfo
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupVersionSummaryPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PublicVarGroupReferCountService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarGroupVersionSummaryDao: PublicVarGroupVersionSummaryDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReferCountService::class.java)
    }

    /**
     * 事务执行模板方法
     * 
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
     * 
     * @param operation 要执行的业务操作
     * @return 操作结果
     */
    private fun <T> executeWithTransaction(
        operation: (DSLContext) -> T
    ): T {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            operation(context)
        }
    }

    /**
     * 批量删除引用并更新引用计数
     * 同时删除变量组引用记录和变量引用记录
     * 
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
     * 
     * @param projectId 项目ID（引用记录所在的当前项目）
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referInfosToDelete 要删除的引用记录列表
     * @param referVersion 引用资源版本（可选，为null时删除所有版本，否则只删除指定版本）
     */
    fun batchRemoveReferInfo(
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referInfosToDelete: List<ResourcePublicVarGroupReferPO>,
        referVersion: Int? = null
    ) {
        if (referInfosToDelete.isEmpty()) {
            val versionInfo = if (referVersion != null) ", referVersion: $referVersion" else ""
            logger.info("No reference found for referId: $referId$versionInfo, skip deletion")
            return
        }

        // 按 (sourceProjectId, groupName) 分组
        // sourceProjectId 用于更新引用计数（变量组实际所在的项目）
        // 如果 sourceProjectId 为空，则使用当前 projectId（非跨项目场景）
        val groupedReferInfos = referInfosToDelete.groupBy {
            Pair(it.sourceProjectId ?: projectId, it.groupName)
        }

        // 按固定顺序排序，保持一致的执行顺序
        val sortedGroups = groupedReferInfos.toList().sortedWith(
            compareBy<Pair<Pair<String, String>, List<ResourcePublicVarGroupReferPO>>> { it.first.first }
                .thenBy { it.first.second }
        )

        sortedGroups.forEach { (key, groupReferInfos) ->
            // sourceProjectId: 变量组所在的项目ID
            val (sourceProjectId, groupName) = key
            // 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护，这里不需要再加锁
            // 在同一个事务中完成引用删除和计数更新
            executeWithTransaction { context ->
                // 1. 删除变量引用记录
                if (referVersion != null) {
                    // 删除指定版本
                    publicVarReferInfoDao.deleteByReferIdAndVersion(
                        dslContext = context,
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        referVersion = referVersion
                    )
                } else {
                    // 删除所有版本
                    publicVarReferInfoDao.deleteByReferId(
                        dslContext = context,
                        projectId = projectId,
                        referId = referId,
                        referType = referType
                    )
                }

                // 2. 删除变量组引用记录
                publicVarGroupReferInfoDao.deleteByReferId(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    referVersion = referVersion
                )

                // 3. 按版本分组，统计每个版本的引用数量，然后减少引用计数
                // 注意：使用 sourceProjectId 更新引用计数，因为计数存储在变量组所在项目
                groupReferInfos
                    .groupingBy { it.version }
                    .eachCount()
                    .forEach { (version, count) ->
                        decrementReferCount(
                            context = context,
                            projectId = sourceProjectId,
                            groupName = groupName,
                            version = version,
                            countChange = count
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
     * @param version 版本号（动态版本为-1）
     * @param countChange 增加的数量
     */
    fun incrementReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        val currentTime = LocalDateTime.now()

        // 检查是否存在该版本的概要信息
        val existingSummary = publicVarGroupVersionSummaryDao.getByGroupNameAndVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version
        )

        if (existingSummary != null) {
            // 使用增量更新
            publicVarGroupVersionSummaryDao.incrementReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = version,
                countChange = countChange,
                modifier = SYSTEM
            )
        } else {
            // 创建新的版本概要记录
            val summaryPO = PublicVarGroupVersionSummaryPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY").data ?: 0,
                projectId = projectId,
                groupName = groupName,
                version = version,
                referCount = countChange,
                creator = SYSTEM,
                modifier = SYSTEM,
                createTime = currentTime,
                updateTime = currentTime
            )
            publicVarGroupVersionSummaryDao.save(
                dslContext = context,
                po = summaryPO
            )
        }
    }

    /**
     * 减少引用计数（增量更新，确保不会变为负数）
     * 
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（动态版本为-1）
     * @param countChange 减少的数量
     */
    fun decrementReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        val existingSummary = publicVarGroupVersionSummaryDao.getByGroupNameAndVersion(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version
        )

        if (existingSummary == null) {
            logger.warn(
                "Summary record not found for decrement, projectId: $projectId, " +
                        "groupName: $groupName, version: $version"
            )
            return
        }

        val newCount = existingSummary.referCount - countChange
        if (newCount < 0) {
            // 将计数设置为0
            publicVarGroupVersionSummaryDao.updateReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = version,
                referCount = 0,
                modifier = SYSTEM
            )
        } else {
            // 使用增量减少
            publicVarGroupVersionSummaryDao.incrementReferCount(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = version,
                countChange = -countChange,
                modifier = SYSTEM
            )
        }
    }

    /**
     * 批量更新引用和计数
     * 
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
     * 
     * @param projectId 当前项目ID（用于删除记录）
     * @param changeInfos 变量组版本变化信息列表
     */
    fun batchUpdateReferWithCount(
        projectId: String,
        changeInfos: List<VarGroupVersionChangeInfo>
    ) {
        if (changeInfos.isEmpty()) {
            return
        }

        // 按 sourceProjectId + groupName 排序，保持一致的执行顺序
        val sortedChangeInfos = changeInfos.sortedWith(
            compareBy<VarGroupVersionChangeInfo> { it.sourceProjectId }
                .thenBy { it.groupName }
        )

        // 依次处理每个变量组
        // 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护
        sortedChangeInfos.forEach { changeInfo ->
            executeWithTransaction { dslCtx ->
                logger.info(
                    "Processing variable group reference update: " +
                            "sourceProjectId=${changeInfo.sourceProjectId}, groupName=${changeInfo.groupName}, " +
                            "referId=${changeInfo.referId}, referType=${changeInfo.referType}, " +
                            "referVersion=${changeInfo.referVersion}, " +
                            "hasDelete=${changeInfo.referInfoToDelete != null}, " +
                            "hasAdd=${changeInfo.referInfoToAdd != null}"
                )

                // 1. 删除变量引用记录
                changeInfo.referInfoToDelete?.let { deleteInfo ->
                    publicVarReferInfoDao.batchDeleteByReferIdAndGroups(
                        dslContext = dslCtx,
                        projectId = projectId,
                        referId = changeInfo.referId,
                        referType = changeInfo.referType,
                        groupNames = listOf(deleteInfo.groupName),
                        referVersion = changeInfo.referVersion
                    )
                }

                // 2. 删除变量组引用记录
                changeInfo.referInfoToDelete?.let { deleteInfo ->
                    publicVarGroupReferInfoDao.batchDeleteByReferIdAndGroups(
                        dslContext = dslCtx,
                        projectId = projectId,
                        referId = changeInfo.referId,
                        referType = changeInfo.referType,
                        groupNames = listOf(deleteInfo.groupName),
                        referVersion = changeInfo.referVersion
                    )
                }

                // 3. 新增变量组引用记录
                changeInfo.referInfoToAdd?.let { addInfo ->
                    publicVarGroupReferInfoDao.batchSave(
                        dslContext = dslCtx,
                        resourcePublicVarGroupReferPOS = listOf(addInfo)
                    )
                }

                // 4. 更新引用计数
                // 根据countChange更新引用计数
                if (changeInfo.countChange > 0) {
                    // 正数：增加引用计数
                    changeInfo.referInfoToAdd?.let { addInfo ->
                        incrementReferCount(
                            context = dslCtx,
                            projectId = changeInfo.sourceProjectId,
                            groupName = changeInfo.groupName,
                            version = addInfo.version,
                            countChange = changeInfo.countChange
                        )
                    }
                } else if (changeInfo.countChange < 0) {
                    // 负数：减少引用计数
                    changeInfo.referInfoToDelete?.let { deleteInfo ->
                        decrementReferCount(
                            context = dslCtx,
                            projectId = changeInfo.sourceProjectId,
                            groupName = changeInfo.groupName,
                            version = deleteInfo.version,
                            countChange = -changeInfo.countChange
                        )
                    }
                }
            }
        }
        logger.info("Successfully batch updated ${changeInfos.size} variable group references")
    }
}
