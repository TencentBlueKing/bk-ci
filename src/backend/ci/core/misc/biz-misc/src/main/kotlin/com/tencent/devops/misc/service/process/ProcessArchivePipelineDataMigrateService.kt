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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.FAIL_MSG
import com.tencent.devops.common.api.constant.KEY_ARCHIVE
import com.tencent.devops.common.api.constant.KEY_PIPELINE_ID
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.constant.OPERATE
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.factory.MigrationStrategyFactory
import com.tencent.devops.misc.lock.MigrationLock
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.DeleteDataParam
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.misc.task.PipelineMigrationTask
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineYamlResource
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.PipelineOperationLog
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class ProcessArchivePipelineDataMigrateService @Autowired constructor(
    private val dslContext: DSLContext,
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private var archiveShardingDslContext: DSLContext,
    private val processDao: ProcessDao,
    processDataMigrateDao: ProcessDataMigrateDao,
    private val processDataDeleteService: ProcessDataDeleteService,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val authResourceApi: AuthResourceApi
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessArchivePipelineDataMigrateService::class.java)
        private const val MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE = "MIGRATE_PROCESS_PIPELINE_DATA_FAIL_TEMPLATE"
        private const val MIGRATE_PROCESS_PIPELINE_DATA_SUCCESS_TEMPLATE =
            "MIGRATE_PROCESS_PIPELINE_DATA_SUCCESS_TEMPLATE"
    }

    // 策略工厂
    private val migrationStrategyFactory = MigrationStrategyFactory(processDataMigrateDao)

    /**
     * 迁移process数据库数据
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param cancelFlag 是否取消正在运行的构建
     * @param sendMsgFlag 是否发送消息
     */
    fun migrateData(
        userId: String,
        projectId: String,
        pipelineId: String,
        cancelFlag: Boolean = false,
        sendMsgFlag: Boolean = true
    ) {
        val archiveDbShardingRoutingRule =
            client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.ARCHIVE_DB
            ).data
        val migrationLock = MigrationLock(redisOperation, projectId, pipelineId)
        val migratePipelineDataParam = MigratePipelineDataParam(
            projectId = projectId,
            pipelineId = pipelineId,
            cancelFlag = cancelFlag,
            dslContext = dslContext,
            migratingShardingDslContext = archiveShardingDslContext,
            processDao = processDao,
            migrationStrategyFactory = migrationStrategyFactory,
            archiveFlag = true
        )
        // 执行迁移前的逻辑
        try {
            doPreMigrationBus(
                userId = userId,
                archiveDbShardingRoutingRule = archiveDbShardingRoutingRule,
                projectId = projectId,
                pipelineId = pipelineId,
                migrationLock = migrationLock
            )
        } catch (ignored: Throwable) {
            val errorMsg = ignored.message
            logger.warn("migrateData project:[$projectId],pipeline[$pipelineId] doPreMigrationBus fail", ignored)
            cleanupMigrationState(pipelineId)
            sendMigrateProcessDataFailMsg(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                errorMsg = errorMsg,
                sendMsgFlag = sendMsgFlag
            )
            throw ignored
        }
        try {
            // 迁移流水线数据
            PipelineMigrationTask(migratePipelineDataParam).run()
            // 执行迁移完成后的逻辑
            doAfterMigrationBus(
                projectId = projectId,
                archiveDbShardingRoutingRule = archiveDbShardingRoutingRule,
                pipelineId = pipelineId,
                migrationLock = migrationLock,
                userId = userId,
                sendMsgFlag = sendMsgFlag
            )
        } catch (ignored: Throwable) {
            val errorMsg = ignored.message
            logger.warn("migrateData project:[$projectId],pipeline[$pipelineId] run task fail", ignored)
            doMigrationErrorBus(
                archiveDbShardingRoutingRule = archiveDbShardingRoutingRule,
                projectId = projectId,
                pipelineId = pipelineId,
                migrationLock = migrationLock,
                userId = userId,
                errorMsg = errorMsg,
                sendMsgFlag = sendMsgFlag
            )
            throw ignored
        } finally {
            cleanupMigrationState(pipelineId)
        }
    }

    private fun cleanupMigrationState(pipelineId: String) {
        // 从正在迁移的流水线集合移除该流水线
        redisOperation.removeSetMember(
            key = BkApiUtil.getMigratingPipelinesRedisKey(SystemModuleEnum.PROCESS.name),
            item = pipelineId
        )
        // 解锁流水线,允许用户发起新构建等操作
        redisOperation.removeSetMember(BkApiUtil.getApiAccessLimitPipelinesKey(), pipelineId)
    }

    private fun doMigrationErrorBus(
        archiveDbShardingRoutingRule: ShardingRoutingRule?,
        projectId: String,
        pipelineId: String,
        migrationLock: MigrationLock,
        userId: String,
        errorMsg: String?,
        sendMsgFlag: Boolean
    ) {
        try {
            if (archiveDbShardingRoutingRule != null) {
                val deleteDataParam = DeleteDataParam(
                    dslContext = archiveShardingDslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    lock = migrationLock,
                    archivePipelineFlag = true,
                    targetClusterName = archiveDbShardingRoutingRule.clusterName,
                    targetDataSourceName = archiveDbShardingRoutingRule.dataSourceName
                )
                processDataDeleteService.deleteProcessData(deleteDataParam)
            }
        } catch (ignored: Throwable) {
            logger.warn("migrateData project:[$projectId],pipeline[$pipelineId] doMigrationErrorBus fail", ignored)
            sendMigrateProcessDataFailMsg(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                errorMsg = ignored.message,
                sendMsgFlag = sendMsgFlag
            )
            throw ignored
        }
        // 迁移流水线数据失败发送失败消息通知用户
        sendMigrateProcessDataFailMsg(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            errorMsg = errorMsg,
            sendMsgFlag = sendMsgFlag
        )
    }

    private fun doAfterMigrationBus(
        projectId: String,
        archiveDbShardingRoutingRule: ShardingRoutingRule?,
        pipelineId: String,
        migrationLock: MigrationLock,
        userId: String,
        sendMsgFlag: Boolean
    ) {
        var tmpArchiveDbShardingRoutingRule = archiveDbShardingRoutingRule
        val originDbShardingRoutingRule =
            client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.DB
            ).data
        if (tmpArchiveDbShardingRoutingRule != null) {
            tmpArchiveDbShardingRoutingRule =
                client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                    routingName = projectId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    ruleType = ShardingRuleTypeEnum.ARCHIVE_DB
                ).data
        }
        if (originDbShardingRoutingRule != null && tmpArchiveDbShardingRoutingRule != null) {
            val sourceClusterName = originDbShardingRoutingRule.clusterName
            val targetClusterName = tmpArchiveDbShardingRoutingRule.clusterName
            try {
                val channelCode = processDao.getPipelineInfoByPipelineId(
                    dslContext = dslContext, projectId = projectId, pipelineId = pipelineId
                )?.channel?.let {
                    ChannelCode.valueOf(it)
                } ?: ChannelCode.BS
                migrationLock.lock()
                client.get(ServicePipelineResource::class).delete(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    channelCode = channelCode,
                    checkFlag = false
                )
                // 删除流水线权限相关的数据
                authResourceApi.deleteResource(
                    serviceCode = pipelineAuthServiceCode,
                    resourceType = AuthResourceType.PIPELINE_DEFAULT,
                    projectCode = projectId,
                    resourceCode = pipelineId
                )
                // 删除原库的数据
                val deleteDataParam = DeleteDataParam(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    targetClusterName = targetClusterName,
                    targetDataSourceName = tmpArchiveDbShardingRoutingRule.dataSourceName
                )
                processDataDeleteService.deleteProcessData(deleteDataParam)
                // 添加操作日志
                val id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("T_PIPELINE_OPERATION_LOG").data
                processDao.addPipelineOperationLog(
                    dslContext = archiveShardingDslContext,
                    pipelineOperationLog = PipelineOperationLog(
                        id = id,
                        operator = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = 1,
                        operationLogType = OperationLogType.PIPELINE_ARCHIVE,
                        params = "",
                        operateTime = System.currentTimeMillis(),
                        description = null
                    )
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
                        targetDataSourceName = tmpArchiveDbShardingRoutingRule.dataSourceName,
                        targetDataTag = KEY_ARCHIVE
                    )
                )
            } finally {
                migrationLock.unlock()
            }
            // 发送迁移成功消息
            sendMigrateProcessDataSuccessMsg(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                sendMsgFlag = sendMsgFlag
            )
        }
    }

    private fun doPreMigrationBus(
        userId: String,
        archiveDbShardingRoutingRule: ShardingRoutingRule?,
        projectId: String,
        pipelineId: String,
        migrationLock: MigrationLock
    ) {
        archiveDbShardingRoutingRule?.let { rule ->
            val queryParam = ProjectDataMigrateHistoryQueryParam(
                projectId = projectId,
                pipelineId = pipelineId,
                moduleCode = SystemModuleEnum.PROCESS,
                targetClusterName = rule.clusterName,
                targetDataSourceName = rule.dataSourceName
            )
            migrationLock.use {
                migrationLock.lock()
                // 判断流水线数据是否能迁移
                if (!projectDataMigrateHistoryService.isDataCanMigrate(queryParam)) {
                    throw ErrorCodeException(
                        errorCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                        params = arrayOf(projectId),
                        defaultMessage = I18nUtil.getCodeLanMessage(
                            messageCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                            params = arrayOf(projectId)
                        )
                    )
                }
            }
        }
        // 判断如果处于PAC模式下的yaml文件是否在默认分支已删除
        client.get(ServicePipelineYamlResource::class)
            .yamlExistInDefaultBranch(userId, projectId, pipelineId)
            .data
            .takeIf { it == true }
            ?.let {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_ARCHIVE_PAC_PIPELINE_YAML_EXIST,
                    params = arrayOf(pipelineId),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = CommonMessageCode.ERROR_ARCHIVE_PAC_PIPELINE_YAML_EXIST,
                        params = arrayOf(pipelineId)
                    )
                )
            }
        // 把流水线加入正在迁移流水线集合中
        redisOperation.addSetValue(
            key = BkApiUtil.getMigratingPipelinesRedisKey(SystemModuleEnum.PROCESS.name),
            item = pipelineId
        )
        // 锁定流水线,不允许用户发起新构建等操作
        redisOperation.addSetValue(BkApiUtil.getApiAccessLimitPipelinesKey(), pipelineId)
    }

    private fun sendMigrateProcessDataSuccessMsg(
        projectId: String,
        pipelineId: String,
        userId: String,
        sendMsgFlag: Boolean
    ) {
        if (!sendMsgFlag) {
            // 不发送消息
            return
        }
        val titleParams = mapOf(
            OPERATE to I18nUtil.getCodeLanMessage(KEY_ARCHIVE),
            KEY_PROJECT_ID to projectId,
            KEY_PIPELINE_ID to pipelineId
        )
        val bodyParams = mapOf(
            OPERATE to I18nUtil.getCodeLanMessage(KEY_ARCHIVE),
            KEY_PROJECT_ID to projectId,
            KEY_PIPELINE_ID to pipelineId
        )
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PIPELINE_DATA_SUCCESS_TEMPLATE,
            receivers = mutableSetOf(userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
        try {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.warn(
                "migrateProjectData project:[$projectId] pipeline:[$pipelineId] send msg" +
                    " template(MIGRATE_PROCESS_PIPELINE_DATA_SUCCESS_TEMPLATE) fail!"
            )
        }
    }

    private fun sendMigrateProcessDataFailMsg(
        projectId: String,
        pipelineId: String,
        userId: String,
        errorMsg: String?,
        sendMsgFlag: Boolean
    ) {
        if (!sendMsgFlag) {
            // 不发送消息
            return
        }
        val titleParams = mapOf(
            OPERATE to I18nUtil.getCodeLanMessage(KEY_ARCHIVE),
            KEY_PROJECT_ID to projectId,
            KEY_PIPELINE_ID to pipelineId
        )
        val bodyParams = mapOf(
            OPERATE to I18nUtil.getCodeLanMessage(KEY_ARCHIVE),
            KEY_PROJECT_ID to projectId,
            KEY_PIPELINE_ID to pipelineId,
            FAIL_MSG to (errorMsg ?: "")
        )
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
