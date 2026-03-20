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
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
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
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
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
     * 每个变量组在独立事务中处理——变量组之间数据独立，无跨组一致性约束，
     * 独立事务可实现故障隔离，避免无关变量组被牵连回滚
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
     * @param projectId 项目ID（引用记录所在的当前项目）
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referInfosToDelete 要删除的引用记录列表
     * @param referVersion 引用资源版本（可选，为null时删除所有版本，否则只删除指定版本）
     */
    fun batchRemoveReferInfo(
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referInfosToDelete: List<ResourcePublicVarGroupReferPO>,
        referVersion: Int? = null
    ) {
        if (referInfosToDelete.isEmpty()) {
            val versionInfo = if (referVersion != null) ", referVersion: $referVersion" else ""
            logger.info("No reference found for referId: $referId$versionInfo, skip deletion")
            return
        }

        // 按 (projectId, groupName) 分组
        val groupedReferInfos = referInfosToDelete.groupBy {
            Pair(projectId, it.groupName)
        }

        // 按固定顺序排序，保持一致的执行顺序
        val sortedGroups = groupedReferInfos.toList().sortedWith(
            compareBy<Pair<Pair<String, String>, List<ResourcePublicVarGroupReferPO>>> { it.first.first }
                .thenBy { it.first.second }
        )

        sortedGroups.forEach { (key, groupReferInfos) ->
            val (groupProjectId, groupName) = key
            // 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护，这里不需要再加锁
            // 在同一个事务中完成单个变量组的引用删除和计数更新
            executeWithTransaction { context ->
                // 1. 删除当前变量组的变量引用记录（按 groupName 隔离）
                publicVarReferInfoDao.deleteByReferIdAndGroup(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = referVersion
                )

                // 2. 删除当前变量组的引用记录（按 groupName 隔离）
                publicVarGroupReferInfoDao.deleteByReferIdAndGroup(
                    dslContext = context,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = referVersion
                )

                // 3. 按版本分组，统计每个版本的引用数量，然后减少引用计数
                groupReferInfos
                    .groupingBy { it.version }
                    .eachCount()
                    .forEach { (version, count) ->
                        decrementReferCount(
                            context = context,
                            projectId = groupProjectId,
                            groupName = groupName,
                            version = version,
                            countChange = count
                        )
                    }
            }
        }
    }

    /**
     * 增加引用计数
     * 优化策略：先尝试 UPDATE 增量累加（热路径，记录已存在时零 RPC 开销），
     * 仅在 UPDATE 返回 0 行（首次创建）时才生成分布式 ID 并执行原子 upsert（防并发安全）
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
        // 热路径：记录已存在，直接 UPDATE 累加，无需生成 ID，零 RPC
        val updatedRows = publicVarGroupVersionSummaryDao.incrementReferCount(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version,
            countChange = countChange,
            modifier = SYSTEM
        )
        if (updatedRows == 0) {
            // 冷路径：记录不存在，生成 ID 并执行原子 upsert
            val currentTime = LocalDateTime.now()
            val id = client.get(ServiceAllocIdResource::class)
                .generateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY").data
                ?: throw IllegalStateException(
                    "Failed to generate segment id for version summary"
                )
            val summaryPO = PublicVarGroupVersionSummaryPO(
                id = id,
                projectId = projectId,
                groupName = groupName,
                version = version,
                referCount = countChange,
                creator = SYSTEM,
                modifier = SYSTEM,
                createTime = currentTime,
                updateTime = currentTime
            )
            publicVarGroupVersionSummaryDao.saveOrIncrementReferCount(
                dslContext = context,
                po = summaryPO
            )
        }
    }

    /**
     * 减少引用计数（原子操作，确保不会变为负数）
     *
     * 直接使用 DAO 层的原子 incrementReferCount（传负数）实现递减，
     * 无需先 SELECT 再判断——消除了 TOCTOU 竞态窗口。
     * DAO 层在 countChange < 0 时自动附加 WHERE REFER_COUNT + countChange >= 0 条件，
     * 保证计数不会变为负数；若条件不满足则 UPDATE 返回 0 行（即跳过）。
     *
     * @param context 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（动态版本为-1）
     * @param countChange 减少的数量（正数，方法内部取反）
     */
    fun decrementReferCount(
        context: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        val updatedRows = publicVarGroupVersionSummaryDao.incrementReferCount(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version,
            countChange = -countChange,
            modifier = SYSTEM
        )
        if (updatedRows == 0) {
            logger.warn(
                "Decrement skipped (record not found or count would go negative), " +
                        "projectId: $projectId, groupName: $groupName, " +
                        "version: $version, countChange: $countChange"
            )
        }
    }

    /**
     * 批量更新引用和计数
     * 每个变量组在独立事务中处理——变量组之间数据独立，无跨组一致性约束，
     * 独立事务可实现故障隔离，避免无关变量组被牵连回滚
     * 注意：该方法不提供锁保护，因为通常由外层（PublicVarGroupReferManageService）已经提供了锁保护。
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

        // 按 groupName 排序，保持一致的执行顺序
        val sortedChangeInfos = changeInfos.sortedBy { it.groupName }

        // 依次处理每个变量组
        // 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护
        sortedChangeInfos.forEach { changeInfo ->
            executeWithTransaction { dslCtx ->
                logger.info(
                    "Processing variable group reference update: " +
                            "projectId=$projectId, groupName=${changeInfo.groupName}, " +
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

                // 4. 更新引用计数（两个操作互相独立，版本切换场景下两者均执行）
                changeInfo.referInfoToDelete?.let { deleteInfo ->
                    decrementReferCount(
                        context = dslCtx,
                        projectId = projectId,
                        groupName = changeInfo.groupName,
                        version = deleteInfo.version,
                        countChange = 1
                    )
                }
                changeInfo.referInfoToAdd?.let { addInfo ->
                    incrementReferCount(
                        context = dslCtx,
                        projectId = projectId,
                        groupName = changeInfo.groupName,
                        version = addInfo.version,
                        countChange = 1
                    )
                }
            }
        }
        logger.info("Successfully batch updated ${changeInfos.size} variable group references")
    }
}