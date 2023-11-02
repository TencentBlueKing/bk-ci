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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.api.constant.FAIL_MSG
import com.tencent.devops.common.api.constant.KEY_ARCHIVE
import com.tencent.devops.common.api.constant.KEY_PIPELINE_ID
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.lock.MigrationLock
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.misc.task.MigratePipelineDataTask
import com.tencent.devops.misc.utils.MiscUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProcessArchivePipelineDataMigrateService @Autowired constructor(
    private val dslContext: DSLContext,
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private var archiveShardingDslContext: DSLContext,
    private val processDao: ProcessDao,
    private val processDataMigrateDao: ProcessDataMigrateDao,
    private val processMigrationDataDeleteService: ProcessMigrationDataDeleteService,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessArchivePipelineDataMigrateService::class.java)
        private const val MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE = "MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE"
    }

    /**
     * 删除process数据库数据
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param cancelFlag 是否取消正在运行的构建
     */
    fun migrateData(
        userId: String,
        projectId: String,
        pipelineId: String,
        cancelFlag: Boolean = false
    ) {
        var archiveDbShardingRoutingRule =
            client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.ARCHIVE_DB
            ).data
        // 重试迁移需删除迁移库的数据以保证迁移接口的幂等性
        val migrationLock = MigrationLock(redisOperation, projectId, pipelineId)
        if (archiveDbShardingRoutingRule != null) {
            processMigrationDataDeleteService.deleteProcessData(
                dslContext = archiveShardingDslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                targetClusterName = archiveDbShardingRoutingRule.clusterName,
                targetDataSourceName = archiveDbShardingRoutingRule.dataSourceName,
                migrationLock = migrationLock
            )
        }
        val migratePipelineDataParam = MigratePipelineDataParam(
            projectId = projectId,
            pipelineId = pipelineId,
            cancelFlag = cancelFlag,
            dslContext = dslContext,
            migratingShardingDslContext = archiveShardingDslContext,
            processDao = processDao,
            processDataMigrateDao = processDataMigrateDao
        )
        try {
            // 迁移流水线数据
            MigratePipelineDataTask(migratePipelineDataParam).run()
            // 把流水线加入未记录已迁移完成流水线集合中
            redisOperation.addSetValue(
                key = MiscUtils.getUnRecordedMigratedPipelinesRedisKey(SystemModuleEnum.PROCESS.name),
                item = pipelineId
            )
        } catch (ignored: Throwable) {
            logger.warn("migrateData project:[$projectId],pipeline[$pipelineId] run task fail", ignored)
            // 迁移流水线数据失败发送失败消息通知用户
            sendMigrateProcessDataFailMsg(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                errorMsg = ignored.message
            )
            return
        }
        val originDbShardingRoutingRule =
            client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.DB
            ).data
        if (archiveDbShardingRoutingRule != null) {
            archiveDbShardingRoutingRule =
                client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                    routingName = projectId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    ruleType = ShardingRuleTypeEnum.ARCHIVE_DB
                ).data
        }
        if (originDbShardingRoutingRule != null && archiveDbShardingRoutingRule != null) {
            val sourceClusterName = originDbShardingRoutingRule.clusterName
            val targetClusterName = archiveDbShardingRoutingRule.clusterName
            // 删除原库的数据
            processMigrationDataDeleteService.deleteProcessData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                targetClusterName = sourceClusterName,
                targetDataSourceName = archiveDbShardingRoutingRule.dataSourceName,
                migrationLock = migrationLock
            )
            // 保存流水线数据迁移成功记录
            projectDataMigrateHistoryService.add(
                userId = userId,
                projectDataMigrateHistory = ProjectDataMigrateHistory(
                    id = UUIDUtil.generate(),
                    projectId = projectId,
                    pipelineId = pipelineId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    sourceClusterName = sourceClusterName,
                    sourceDataSourceName = originDbShardingRoutingRule.dataSourceName,
                    targetClusterName = targetClusterName,
                    targetDataSourceName = archiveDbShardingRoutingRule.dataSourceName,
                    targetDataTag = KEY_ARCHIVE
                )
            )
        }
    }

    private fun sendMigrateProcessDataFailMsg(
        projectId: String,
        pipelineId: String,
        userId: String,
        errorMsg: String?
    ) {
        val titleParams = mapOf(KEY_PROJECT_ID to projectId, KEY_PIPELINE_ID to projectId)
        val bodyParams = mapOf(KEY_PROJECT_ID to projectId, KEY_PIPELINE_ID to projectId, FAIL_MSG to (errorMsg ?: ""))
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE,
            receivers = mutableSetOf(userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
        try {
            // 发送迁移失败消息
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.warn(
                "migrateProjectData project:[$projectId] pipeline:[$pipelineId] send msg" +
                    " template(MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE) fail!"
            )
        }
    }
}
