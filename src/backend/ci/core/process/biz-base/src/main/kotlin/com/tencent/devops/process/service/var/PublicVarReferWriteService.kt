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

import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 公共变量引用写入服务
 *
 * 职责：把 [PublicVarReferInfoService] 计算出来的"需要新增的引用记录"批量写入
 * `T_RESOURCE_PUBLIC_VAR_REFER_INFO` 表。
 *
 * 说明：
 * - 自方案 4 起，`T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY.REFER_COUNT` 字段不再由代码维护，
 *   referCount 统一通过实时 JOIN 聚合查询得出（见 [PublicVarVersionSummaryDao.batchGetActiveReferCount]）。
 *   因此本服务不再承担"更新计数"职责，仅负责引用记录的批量写入。
 * - 本服务不提供锁保护，锁保护由外层（[PublicVarReferInfoService]）统一控制。
 */
@Service
class PublicVarReferWriteService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarReferInfoDao: PublicVarReferInfoDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarReferWriteService::class.java)
    }

    /**
     * 批量新增引用记录（自管理事务版本）
     * @param referInfos 引用信息列表
     */
    fun batchAddRefer(referInfos: List<ResourcePublicVarReferPO>) {
        if (referInfos.isEmpty()) {
            return
        }
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            doBatchAddRefer(context, referInfos)
        }
    }

    /**
     * 批量新增引用记录（使用外部事务上下文）
     * 在外部事务中执行，不再自行开启新事务，确保与引用关系变更在同一事务中完成。
     * @param context 外部事务的数据库上下文
     * @param referInfos 引用信息列表
     */
    fun batchAddReferInTransaction(
        context: DSLContext,
        referInfos: List<ResourcePublicVarReferPO>
    ) {
        if (referInfos.isEmpty()) {
            return
        }
        doBatchAddRefer(context, referInfos)
    }

    /**
     * 批量插入引用记录的内部实现。
     * 按 (projectId, groupName, varName) 固定顺序分批，避免并发场景下的死锁。
     */
    private fun doBatchAddRefer(
        context: DSLContext,
        referInfos: List<ResourcePublicVarReferPO>
    ) {
        // 按版本分组后再按 (projectId, groupName, varName) 排序，保持一致的执行顺序，避免死锁
        val groupedByVersion = referInfos.groupBy { it.version ?: -1 }

        groupedByVersion.forEach { (version, varReferInfosForVersion) ->
            val sortedRecords = varReferInfosForVersion.sortedWith(
                compareBy<ResourcePublicVarReferPO> { it.projectId }
                    .thenBy { it.groupName }
                    .thenBy { it.varName }
            )

            logger.info(
                "Batch insert var refer records, version=$version, size=${sortedRecords.size}"
            )

            publicVarReferInfoDao.batchSave(
                dslContext = context,
                pipelinePublicVarReferPOs = sortedRecords
            )
        }
    }
}
