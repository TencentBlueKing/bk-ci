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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataDeleteDao
import com.tencent.devops.misc.lock.PipelineVersionLock
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProcessDataClearService @Autowired constructor(
    private val dslContext: DSLContext,
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private val archiveShardingDslContext: DSLContext,
    private val processDao: ProcessDao,
    private val processDataDeleteDao: ProcessDataDeleteDao,
    private val redisOperation: RedisOperation,
    private val processRelatedPlatformDataClearService: ProcessRelatedPlatformDataClearService
) {

    private val logger = LoggerFactory.getLogger(ProcessDataClearService::class.java)

    /**
     * 清除流水线数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param archiveFlag 归档标识
     */
    fun clearPipelineData(
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean? = null
    ) {
        val finalDslContext = generateFinalDslContext(archiveFlag)
        val pipelineIds = arrayListOf(pipelineId)
        finalDslContext.transaction { t ->
            // 所有 DAO 删除操作统一走事务上下文 context，保证流水线本体清理的原子性；
            // 流水线维度的关联表清单内聚在 DAO 聚合方法里，新增表只需在 DAO 处补一行
            val context = DSL.using(t)
            processDataDeleteDao.deletePipelineRelatedData(
                dslContext = context,
                projectId = projectId,
                pipelineIds = pipelineIds,
                archiveFlag = archiveFlag,
                broadcastTableDeleteFlag = true
            )
            if (archiveFlag != true) {
                // 添加删除记录，插入要实现幂等
                processDao.addPipelineDataClear(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
            }
        }
        processRelatedPlatformDataClearService.cleanBuildData(projectId, pipelineId)
    }

    private fun generateFinalDslContext(archiveFlag: Boolean?): DSLContext {
        val finalDslContext = if (archiveFlag == true) {
            archiveShardingDslContext
        } else {
            dslContext
        }
        return finalDslContext
    }

    /**
     * 清除流水线基础构建数据
     * @param projectId 项目ID
     * @param buildId 构建ID
     *
     * 注意：这里刻意不包裹 dslContext.transaction。
     *   1) 四张表都按 (PROJECT_ID, BUILD_ID) 维度删除，互相之间无跨表一致性要求；
     *   2) 删除操作天然幂等，单步失败可以由下一轮 cron / 重试补救；
     *   3) 包成长事务会让多张表的行锁/索引页锁在整段时间内持有，
     *      与运行时构建写入路径（如 T_PIPELINE_BUILD_VAR / T_PIPELINE_BUILD_TASK）
     *      共享同一索引页时极易引发热点等待。
     */
    fun clearBaseBuildData(projectId: String, buildId: String) {
        val buildIds = arrayListOf(buildId)
        processDataDeleteDao.deletePipelineBuildTask(dslContext, projectId, buildIds)
        processDataDeleteDao.deletePipelineBuildVar(dslContext, projectId, buildIds)
        processDataDeleteDao.deletePipelineBuildContainer(dslContext, projectId, buildIds)
        processDataDeleteDao.deletePipelineBuildStage(dslContext, projectId, buildIds)
    }

    /**
     * 清除流水线其它构建数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param archiveFlag 归档标识
     *
     * 事务策略：
     *   1) deleteBuildRelatedData 涉及 10+ 张按 BUILD_ID 维度的关联表（detail / pause_value /
     *      webhook_build_parameter / report / trigger_review / build_record_* 等），
     *      互相之间无跨表一致性要求，单步失败可由下一轮清理补救，**不再纳入事务**；
     *   2) 仅 deletePipelineBuildHistory + addBuildHisDataClear + updatePipelineVersionReferInfo
     *      三步存在强一致诉求（历史记录是否删除、是否记账、版本引用计数三者必须一致），
     *      保留一个最小的短事务包裹。
     */
    @Suppress("LongMethod")
    fun clearOtherBuildData(
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean? = null
    ) {
        val finalDslContext = generateFinalDslContext(archiveFlag)
        val buildIds = arrayListOf(buildId)
        // 1) 按 BUILD_ID 维度的关联表删除：无跨表一致性，幂等，无事务直接逐条提交
        processDataDeleteDao.deleteBuildRelatedData(
            dslContext = finalDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildIds = buildIds,
            archiveFlag = archiveFlag
        )
        if (archiveFlag == true) {
            // 归档场景：BuildHistory 直接删除即可，无版本引用计数维护
            processDataDeleteDao.deletePipelineBuildHistory(finalDslContext, projectId, buildId)
            return
        }
        val version = processDao.getPipelineVersionByBuildId(
            dslContext = finalDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
        val pipelineVersionLock = version?.let { PipelineVersionLock(redisOperation, pipelineId, it) }
        try {
            pipelineVersionLock?.lock()
            // 2) 仅 BuildHistory 删除 + 删除记账 + 版本引用计数更新走短事务
            finalDslContext.transaction { t ->
                val context = DSL.using(t)
                val deleteResult = processDataDeleteDao.deletePipelineBuildHistory(context, projectId, buildId)
                if (deleteResult == 0) {
                    // 如果删除的记录数为0则无需执行后面的逻辑
                    logger.warn("Pipeline [$pipelineId] build [$buildId] record deletion failed")
                    return@transaction
                }
                // 添加删除记录，插入要实现幂等
                processDao.addBuildHisDataClear(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                // 无版本信息则无需更新计数
                if (version == null) {
                    logger.warn("Pipeline [$pipelineId] build [$buildId] record no version information")
                    return@transaction
                }
                // 查询流水线版本记录
                val pipelineVersionInfo = processDao.getPipelineVersionSimple(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version
                )
                var referCount = pipelineVersionInfo?.referCount
                referCount = if (referCount == null || referCount < 0) {
                    // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
                    processDao.countBuildNumByVersion(
                        dslContext = context,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = version
                    )
                } else {
                    referCount - 1
                }
                val referFlag = referCount > 0
                // 更新流水线版本关联构建记录信息
                processDao.updatePipelineVersionReferInfo(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version,
                    referCount = referCount,
                    referFlag = referFlag
                )
                logger.info("Update pipeline[$pipelineId] REFER_COUNT for version $version, new count: $referCount")
            }
        } finally {
            pipelineVersionLock?.unlock()
        }
    }

    /**
     * 清除流水线被跳过的任务数据
     * @param projectId 项目ID
     * @param buildId 构建ID
     * @param archiveFlag 归档标识
     */
    fun clearSkipRecordTaskData(
        projectId: String,
        buildId: String,
        archiveFlag: Boolean? = null
    ) {
        processDataDeleteDao.deletePipelineBuildRecordTask(
            dslContext = generateFinalDslContext(archiveFlag),
            projectId = projectId,
            buildIds = arrayListOf(buildId),
            skipTaskDeleteFlag = true
        )
    }
}
