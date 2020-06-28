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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.misc.cron

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.config.MiscBuildDataClearConfig
import com.tencent.devops.misc.service.PipelineHistoryDataClearService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.StringBuilder
import java.util.Date

@Component
@DependsOn("springContextUtil")
class PipelineBuildHistoryDataClearJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val miscBuildDataClearConfig: MiscBuildDataClearConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildHistoryDataClearJob::class.java)
        private const val LOCK_KEY = "pipelineBuildHistoryDataClear"
        private val pipelineHistoryDataClearService =
            SpringContextUtil.getBean(PipelineHistoryDataClearService::class.java)
        private val dataBaseInfo = pipelineHistoryDataClearService.getDataBaseInfo()
        private val PROJECT_DATA_BASE_NAME = dataBaseInfo[pipelineHistoryDataClearService.projectDbKey]
        private val PROCESS_DATA_BASE_NAME = dataBaseInfo[pipelineHistoryDataClearService.processDbKey]
        private val tableInfo = pipelineHistoryDataClearService.getTableInfo()
        private val PROJECT_TABLE_NAME = tableInfo[pipelineHistoryDataClearService.projectTableKey]
        private val PIPELINE_INFO_TABLE_NAME = tableInfo[pipelineHistoryDataClearService.pipelineInfoTableKey]
        private val PIPELINE_BUILD_HISTORY_TABLE_NAME =
            tableInfo[pipelineHistoryDataClearService.pipelineBuildHistoryTableKey]
        private const val PIPELINE_BUILD_HISTORY_PAGE_SIZE = 100
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY =
            "pipeline:build:history:data:clear:project:id"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY =
            "pipeline:build:history:data:clear:project:list"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_PAGE_KEY =
            "pipeline:build:history:data:clear:project:list:page"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 12000)
    fun pipelineBuildHistoryDataClear() {
        if (!miscBuildDataClearConfig.switch.toBoolean()) {
            // 如果清理构建历史数据开关关闭，则不清理
            return
        }
        logger.info("pipelineBuildHistoryDataClear start")
        val lock = RedisLock(redisOperation, LOCK_KEY, 100)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            // 查询t_project表中的项目数据处理
            val projectListConfig = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY)
            // 组装查询项目的条件
            val projectConditionSqlBuilder = StringBuilder("1=1")
            if (!projectListConfig.isNullOrBlank()) {
                projectConditionSqlBuilder.append(" and english_name in (")
                val projectList = projectListConfig!!.split(",")
                projectList.forEach {
                    projectConditionSqlBuilder.append("'$it',")
                }
                // 删除最后一个逗号
                projectConditionSqlBuilder.deleteCharAt(projectConditionSqlBuilder.length - 1)
                projectConditionSqlBuilder.append(")")
            }
            var handleProjectPrimaryId = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY)?.toLong()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId =
                    dslContext.select(DSL.field("min(id)")).from("$PROJECT_DATA_BASE_NAME.$PROJECT_TABLE_NAME")
                        .where(projectConditionSqlBuilder.toString())
                        .fetchOne(0, Long::class.java) ?: 0L
            } else {
                val maxProjectPrimaryId =
                    dslContext.select(DSL.field("max(id)")).from("$PROJECT_DATA_BASE_NAME.$PROJECT_TABLE_NAME")
                        .where(projectConditionSqlBuilder.toString())
                        .fetchOne(0, Long::class.java)
                if (handleProjectPrimaryId >= maxProjectPrimaryId) {
                    // 已经清理完全部项目的流水线的过期构建记录，再重新开始清理
                    redisOperation.delete(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY)
                    if (!projectListConfig.isNullOrBlank()) {
                        redisOperation.delete(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_PAGE_KEY)
                    }
                    logger.info("pipelineBuildHistoryDataClear reStart")
                    return
                }
            }
            val maxEveryProjectHandleNum = miscBuildDataClearConfig.maxEveryProjectHandleNum.toInt()
            val projectBaseQueryStep = dslContext.select().from("$PROJECT_DATA_BASE_NAME.$PROJECT_TABLE_NAME")
            var maxHandleProjectPrimaryId = handleProjectPrimaryId ?: 0L
            if (projectListConfig.isNullOrBlank()) {
                maxHandleProjectPrimaryId = handleProjectPrimaryId + maxEveryProjectHandleNum
                projectConditionSqlBuilder.append(" and (id >$handleProjectPrimaryId and id<=$maxHandleProjectPrimaryId)")
                projectBaseQueryStep.where(projectConditionSqlBuilder.toString())
            } else {
                val page = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_PAGE_KEY)?.toInt() ?: 1
                projectBaseQueryStep.where(projectConditionSqlBuilder.toString()).orderBy(DSL.field("id").asc())
                    .limit((page - 1) * maxEveryProjectHandleNum, maxEveryProjectHandleNum)
            }
            val projectInfoRecords = projectBaseQueryStep.fetch()
            // 根据项目依次查询T_PIPELINE_INFO表中的流水线数据处理
            projectInfoRecords.forEach { projectInfo ->
                val projectPrimaryId = projectInfo["id"] as Long
                if (projectPrimaryId > maxHandleProjectPrimaryId) {
                    maxHandleProjectPrimaryId = projectPrimaryId
                }
                val projectId = projectInfo["english_name"] as String
                val pipelineInfoRecords =
                    dslContext.select().from("$PROCESS_DATA_BASE_NAME.$PIPELINE_INFO_TABLE_NAME")
                        .where("PROJECT_ID='$projectId'").fetch()
                pipelineInfoRecords.forEach { pipelineInfo ->
                    // 根据流水线ID依次查询T_PIPELINE_BUILD_HISTORY表中二个月前的构建记录
                    val pipelineId = pipelineInfo["PIPELINE_ID"] as String
                    val currentDate = DateTimeUtil.formatDate(Date())
                    val monthRange = miscBuildDataClearConfig.monthRange
                    val pastConditionSql =
                        "PIPELINE_ID='$pipelineId' AND START_TIME < SUBDATE('$currentDate', INTERVAL $monthRange MONTH)"
                    logger.info("pipelineBuildHistoryPastDataClear start..............")
                    cleanBuildHistoryData(
                        pipelineId = pipelineId,
                        conditionSql = pastConditionSql,
                        projectId = projectId,
                        isCompletelyDelete = false
                    )
                    // 判断构建记录是否超过系统展示的最大数量，如果超过则需清理超量的数据
                    val maxPipelineBuildNum = dslContext.select(DSL.field("MAX(BUILD_NUM)"))
                        .from("$PROCESS_DATA_BASE_NAME.$PIPELINE_BUILD_HISTORY_TABLE_NAME")
                        .where("PROJECT_ID='$projectId' AND PIPELINE_ID='$pipelineId'")
                        .fetchOne(0, Long::class.java)
                    val maxKeepNum = miscBuildDataClearConfig.maxKeepNum.toInt()
                    if (maxPipelineBuildNum > maxKeepNum) {
                        val recentConditionSql =
                            "PIPELINE_ID='$pipelineId' AND BUILD_NUM <= ${maxPipelineBuildNum - maxKeepNum}"
                        logger.info("pipelineBuildHistoryRecentDataClear start.............")
                        cleanBuildHistoryData(
                            pipelineId = pipelineId,
                            conditionSql = recentConditionSql,
                            projectId = projectId,
                            isCompletelyDelete = true
                        )
                    }
                }
            }
            // 将当前已处理完的最大项目Id存入redis
            redisOperation.set(
                key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY,
                value = maxHandleProjectPrimaryId.toString(),
                expired = false
            )
            if (!projectListConfig.isNullOrBlank()) {
                // 如果是指定项目，需把处理项目列表的页码放入redis
                val page = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_PAGE_KEY)?.toInt() ?: 1
                redisOperation.set(
                    key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_PAGE_KEY,
                    value = (page + 1).toString(),
                    expired = false
                )
            }
        } catch (t: Throwable) {
            logger.warn("pipelineBuildHistoryDataClear failed", t)
        } finally {
            lock.unlock()
        }
    }

    private fun cleanBuildHistoryData(
        pipelineId: String,
        conditionSql: String,
        projectId: String,
        isCompletelyDelete: Boolean
    ) {
        val totalBuildCount =
            dslContext.selectCount().from("$PROCESS_DATA_BASE_NAME.$PIPELINE_BUILD_HISTORY_TABLE_NAME")
                .where(conditionSql)
                .fetchOne(0, Long::class.java)
        logger.info("pipelineBuildHistoryDataClear pipelineId:$pipelineId,totalBuildCount:$totalBuildCount")
        var totalHandleNum = 0
        while (totalHandleNum < totalBuildCount) {
            logger.info("pipelineBuildHistoryDataClear pipelineId:$pipelineId,totalBuildCount:$totalBuildCount,totalHandleNum:$totalHandleNum")
            val baseStep = dslContext.select(DSL.field("BUILD_ID"))
                .from("$PROCESS_DATA_BASE_NAME.$PIPELINE_BUILD_HISTORY_TABLE_NAME")
                .where(conditionSql)
            if (isCompletelyDelete) {
                baseStep.limit(PIPELINE_BUILD_HISTORY_PAGE_SIZE)
            } else {
                baseStep.limit(totalHandleNum, PIPELINE_BUILD_HISTORY_PAGE_SIZE)
            }
            val pipelineHistoryBuildIds = baseStep.fetch()
            pipelineHistoryBuildIds.forEach {
                val buildId = it.value1().toString()
                // 依次删除process表中的相关构建记录(T_PIPELINE_BUILD_HISTORY做为基准表，为了保证构建流水记录删干净，T_PIPELINE_BUILD_HISTORY记录要最后删)
                val batchSqlList = pipelineHistoryDataClearService.getClearSqlList(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    isCompletelyDelete = isCompletelyDelete
                )
                dslContext.batch(batchSqlList).execute()
            }
            totalHandleNum += pipelineHistoryBuildIds.size
        }
    }
}