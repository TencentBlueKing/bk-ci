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

import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.VarGroupVersionChangeInfo
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 变量组引用记录写入服务。
 *
 * 职责：只负责引用关联记录（T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO / 变量引用明细）本身的增删。
 * 引用数概要 `T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY` 不在本服务维护：
 * 它由 `PublicVarGroupReferManageService.refreshGroupLevelSummary` 在同一事务内、按明细"重算覆盖"统一维护，
 * 变量级概要则由 `refreshVarLevelSummary` 在变量明细写入/删除事务内维护，
 * 均保证与明细强一致（不做增量增减，避免误差累积）。类名保留 `Count` 仅为减少改动面，后续可重命名。
 */
@Service
class PublicVarGroupReferCountService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao
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
     * 批量删除引用记录（自行管理事务）
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
        batchRemoveReferInfoInternal(
            transactionContext = null,
            projectId = projectId,
            referId = referId,
            referType = referType,
            referInfosToDelete = referInfosToDelete,
            referVersion = referVersion
        )
    }

    /**
     * 批量删除引用记录（复用外部事务）
     * 当 transactionContext 不为 null 时，所有 DB 操作使用该 context，不另开事务，
     * 确保引用清理与调用方（如 deletePipeline）在同一事务内，要么全成功要么全回滚。
     * @param transactionContext 外部事务的 DSLContext，null 表示自行管理事务（兼容原逻辑）
     */
    fun batchRemoveReferInfo(
        transactionContext: DSLContext,
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
        batchRemoveReferInfoInternal(
            transactionContext = transactionContext,
            projectId = projectId,
            referId = referId,
            referType = referType,
            referInfosToDelete = referInfosToDelete,
            referVersion = referVersion
        )
    }

    private fun batchRemoveReferInfoInternal(
        transactionContext: DSLContext?,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referInfosToDelete: List<ResourcePublicVarGroupReferPO>,
        referVersion: Int?
    ) {
        // 涉及的变量组名集合（按 groupName 去重并排序，保持一致的执行顺序，避免死锁）
        val groupNames = referInfosToDelete.map { it.groupName }.distinct().sorted()

        groupNames.forEach { groupName ->
            // 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护，这里不需要再加锁
            // 若 transactionContext 不为 null，复用外部事务（如 deletePipeline 事务）；
            // 否则每个变量组在独立事务中处理（故障隔离，避免无关变量组被牵连回滚）
            if (transactionContext != null) {
                removeReferInfoForGroup(
                    context = transactionContext,
                    projectId = projectId,
                    referId = referId,
                    referType = referType,
                    groupName = groupName,
                    referVersion = referVersion
                )
            } else {
                executeWithTransaction { context ->
                    removeReferInfoForGroup(
                        context = context,
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        groupName = groupName,
                        referVersion = referVersion
                    )
                }
            }
        }
    }

    /**
     * 对单个变量组执行删除引用记录（变量引用记录 + 变量组引用记录）。
     * 计数体系已下线，这里只删除关联记录本身。
     * 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护，这里不需要再加锁。
     */
    private fun removeReferInfoForGroup(
        context: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int?
    ) {
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
    }

    /**
     * 批量写入变量组引用记录（删除旧引用 + 新增新引用）。
     * 计数体系已下线，本方法只维护引用关联记录本身。
     * 复用调用方事务，保证与 LATEST_FLAG 同步等操作在同一事务内原子提交。
     * 注意：外层（PublicVarGroupReferManageService）已经提供了锁保护。
     * @param context 外部事务上下文
     * @param projectId 当前项目ID（用于删除记录）
     * @param changeInfos 变量组版本变化信息列表
     */
    fun batchUpdateReferInfo(
        context: DSLContext,
        projectId: String,
        changeInfos: List<VarGroupVersionChangeInfo>
    ) {
        if (changeInfos.isEmpty()) {
            return
        }

        // 按 groupName 排序，保持一致的执行顺序
        val sortedChangeInfos = changeInfos.sortedBy { it.groupName }

        sortedChangeInfos.forEach { changeInfo ->
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
                    dslContext = context,
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
                    dslContext = context,
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
                    dslContext = context,
                    resourcePublicVarGroupReferPOS = listOf(addInfo)
                )
            }
        }
        logger.info("Successfully batch updated ${changeInfos.size} variable group references")
    }
}
