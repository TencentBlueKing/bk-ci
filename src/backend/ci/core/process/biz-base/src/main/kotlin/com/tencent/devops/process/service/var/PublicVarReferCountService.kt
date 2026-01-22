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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
import com.tencent.devops.process.constant.ProcessMessageCode.PUBLIC_VAR_REFER_LOCK_KEY_PREFIX
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.pojo.`var`.VarCountUpdateInfo
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 公共变量引用计数服务
 * 提供变量级别的引用计数安全更新机制
 * 使用分布式锁保护，以变量为粒度（project_id + group_name + var_name）
 */
@Service
class PublicVarReferCountService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferCountService::class.java)
    }

    /**
     * 创建分布式锁
     * 锁粒度策略：
     * - 使用变量级别的锁（项目+变量组+变量级别）
     *   锁key格式：prefix:projectId:groupName:varName
     * @param projectId 项目ID（必需）
     * @param groupName 变量组名称（必需）
     * @param varName 变量名称（必需）
     * @return RedisLock实例
     */
    private fun createLock(
        projectId: String,
        groupName: String,
        varName: String
    ): RedisLock {
        val lockKey = "$PUBLIC_VAR_REFER_LOCK_KEY_PREFIX:$projectId:$groupName:$varName"
        return RedisLock(
            redisOperation = redisOperation,
            lockKey = lockKey,
            expiredTimeInSeconds = PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
    }

    /**
     * 带锁保护的事务执行模板方法
     * @param projectId 项目ID（必需）
     * @param groupName 变量组名称（必需）
     * @param varName 变量名称（必需）
     * @param operation 要执行的业务操作
     * @return 操作结果
     */
    fun <T> executeWithLockAndTransaction(
        projectId: String,
        groupName: String,
        varName: String,
        operation: (DSLContext) -> T
    ): T {
        val lock = createLock(
            projectId = projectId,
            groupName = groupName,
            varName = varName
        )
        lock.lock()
        try {
            return dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                operation(context)
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * 批量新增引用并更新引用计数
     * @param referInfos 引用信息列表
     */
    fun batchAddReferWithCount(referInfos: List<ResourcePublicVarReferPO>) {
        if (referInfos.isEmpty()) return

        // 按 (projectId, groupName, varName) 分组
        val groupedReferInfos = referInfos.groupBy { 
            Triple(it.projectId, it.groupName, it.varName)
        }

        // 按固定顺序排序，避免死锁
        val sortedGroups = groupedReferInfos.toList().sortedWith(
            compareBy<Pair<Triple<String, String, String>, List<ResourcePublicVarReferPO>>> { it.first.first }
                .thenBy { it.first.second }
                .thenBy { it.first.third }
        )

        sortedGroups.forEach { (key, varReferInfos) ->
            val (projectId, groupName, varName) = key
            
            // 提取 sourceProjectId，用于锁和计数更新
            // 如果 sourceProjectId 为空，则使用当前 projectId（非跨项目场景）
            val sourceProjectId = varReferInfos.firstOrNull()?.sourceProjectId ?: projectId
            
            executeWithLockAndTransaction(
                projectId = sourceProjectId,  // 使用 sourceProjectId 获取锁
                groupName = groupName,
                varName = varName
            ) { context ->
                logger.info(
                    "Processing variable reference addition: " +
                        "sourceProjectId=$sourceProjectId, groupName=$groupName, varName=$varName, " +
                        "count=${varReferInfos.size}"
                )

                // 1. 批量插入引用记录（使用当前 projectId，引用记录存储在当前项目）
                publicVarReferInfoDao.batchSave(
                    dslContext = context,
                    pipelinePublicVarReferPOs = varReferInfos
                )

                // 2. 按版本分组增量更新引用计数（使用 sourceProjectId，计数存储在变量组所在项目）
                val countByVersion = varReferInfos
                    .groupBy { it.version }
                    .mapValues { it.value.size }

                countByVersion.forEach { (version, count) ->
                    incrementReferCount(
                        context = context,
                        projectId = sourceProjectId,  // 使用 sourceProjectId 更新计数
                        groupName = groupName,
                        varName = varName,
                        version = version ?: -1,
                        countChange = count
                    )
                }
            }
        }
    }

    /**
     * 增加引用计数（增量更新）
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
        val currentTime = LocalDateTime.now()

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
            // 创建新的版本概要记录
            val summaryPO = PublicVarVersionSummaryPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY").data ?: 0,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = countChange,
                creator = SYSTEM,
                modifier = SYSTEM,
                createTime = currentTime,
                updateTime = currentTime
            )
            publicVarVersionSummaryDao.save(
                dslContext = context,
                po = summaryPO
            )
        }
    }

    /**
     * 重新计算引用计数
     * @param version 版本号（null表示重新计算所有版本）
     */
    fun recalculateReferCount(
        projectId: String,
        groupName: String,
        varName: String,
        version: Int? = null
    ) {
        executeWithLockAndTransaction(
            projectId = projectId,
            groupName = groupName,
            varName = varName
        ) { context ->
            if (version != null) {
                // 重新计算指定版本的引用计数
                recalculateSingleVersionReferCount(
                    context = context,
                    projectId = projectId,
                    groupName = groupName,
                    varName = varName,
                    version = version
                )
            } else {
                // 重新计算动态版本的引用计数
                recalculateSingleVersionReferCount(
                    context = context,
                    projectId = projectId,
                    groupName = groupName,
                    varName = varName,
                    version = -1 // 动态版本
                )
            }
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
        val currentTime = LocalDateTime.now()

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
            // 只有当实际有引用时才创建新的版本概要记录
            val summaryPO = PublicVarVersionSummaryPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY").data ?: 0,
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                version = version,
                referCount = actualReferCount,
                creator = SYSTEM,
                modifier = SYSTEM,
                createTime = currentTime,
                updateTime = currentTime
            )
            publicVarVersionSummaryDao.save(
                dslContext = context,
                po = summaryPO
            )
        }
    }

    /**
     * 更新变量维度的引用计数
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
            // 按固定顺序排序，避免死锁
            val sortedVars = varsNeedRecalculate.sortedWith(
                compareBy<VarCountUpdateInfo> { it.projectId }
                    .thenBy { it.groupName }
                    .thenBy { it.varName }
            )

            sortedVars.forEach { varInfo ->
                recalculateReferCount(
                    projectId = varInfo.projectId,
                    groupName = varInfo.groupName,
                    varName = varInfo.varName,
                    version = varInfo.version
                )
            }
        }
    }
}
