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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.FAIL_MSG
import com.tencent.devops.common.api.constant.KEY_PIPELINE_NUM
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.DEFAULT_DATA_SOURCE_NAME
import com.tencent.devops.common.db.pojo.MIGRATING_DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.MIGRATING_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.lock.MigrationLock
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.DeleteMigrationDataParam
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.service.project.DataSourceService
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.misc.task.MigratePipelineDataTask
import com.tencent.devops.misc.utils.MiscUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import jakarta.annotation.PostConstruct

@Suppress("TooManyFunctions", "LongMethod", "LargeClass", "LongParameterList", "ComplexMethod")
@Service
class ProcessDataMigrateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val processDao: ProcessDao,
    private val processDataMigrateDao: ProcessDataMigrateDao,
    private val processMigrationDataDeleteService: ProcessMigrationDataDeleteService,
    private val dataSourceService: DataSourceService,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessDataMigrateService::class.java)
        private const val DEFAULT_THREAD_NUM = 10
        private const val DEFAULT_PAGE_SIZE = 20
        private const val DEFAULT_MIGRATION_TIMEOUT = 2L
        private const val DEFAULT_MIGRATION_MAX_PROJECT_COUNT = 5
        private const val DEFAULT_THREAD_SLEEP_TIMEOUT = 5000L
        private const val SHORT_PAGE_SIZE = 5
        private const val MEDIUM_PAGE_SIZE = 100
        private const val LONG_PAGE_SIZE = 1000
        private const val RETRY_NUM = 3
        private const val MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE = "MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE"
        private const val MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE =
            "MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE"
        private const val MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY = "MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT"
    }

    @Value("\${sharding.migration.timeout:#{2}}")
    private val migrationTimeout: Long = DEFAULT_MIGRATION_TIMEOUT

    @Value("\${sharding.migration.maxProjectCount:#{5}}")
    private val migrationMaxProjectCount: Int = DEFAULT_MIGRATION_MAX_PROJECT_COUNT

    @Value("\${sharding.migration.processDbMicroServices:#{\"process,engine,misc,lambda\"}}")
    private val migrationProcessDbMicroServices = "process,engine,misc,lambda"

    @Value("\${sharding.migration.sourceDbDataDeleteFlag:#{false}}")
    private val migrationSourceDbDataDeleteFlag: Boolean = false

    @Value("\${sharding.migration.processDbUnionClusterFlag:#{true}}")
    private val migrationProcessDbUnionClusterFlag: Boolean = true

    @PostConstruct
    fun init() {
        // 启动的时候重置redis中存储的同时迁移的项目数量，防止因为服务异常停了造成程序执行出错
        redisOperation.setIfAbsent(key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY, value = "0", expired = false)
    }

    fun migrateProjectData(
        userId: String,
        projectId: String,
        cancelFlag: Boolean = false,
        dataTag: String? = null
    ): Boolean {
        val migratingShardingDslContext = try {
            SpringContextUtil.getBean(DSLContext::class.java, MIGRATING_SHARDING_DSL_CONTEXT)
        } catch (ignored: Throwable) {
            logger.warn("migratingShardingDslContext is not exist", ignored)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR,
                defaultMessage = "migratingShardingDslContext is not exist"
            )
        }
        // 查找项目原来分配的数据源
        val sourceDataSourceName = client.get(ServiceShardingRoutingRuleResource::class)
            .getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.DB
            ).data?.dataSourceName ?: return false
        try {
            // 执行迁移前的逻辑
            val (migrateProjectExecuteCountKey, projectExecuteCount, routingRuleMap) = doPreMigrationBus(
                projectId = projectId,
                sourceDataSourceName = sourceDataSourceName,
                dataTag = dataTag
            )
            // 开启异步任务迁移项目的数据
            Executors.newFixedThreadPool(1).submit {
                logger.info("migrateProjectData begin,params:[$userId|$projectId]")
                try {
                    doMigrateProjectDataTask(
                        migratingShardingDslContext = migratingShardingDslContext,
                        userId = userId,
                        projectId = projectId,
                        dataTag = dataTag,
                        cancelFlag = cancelFlag,
                        sourceDataSourceName = sourceDataSourceName,
                        routingRuleMap = routingRuleMap,
                        projectExecuteCount = projectExecuteCount,
                        migrateProjectExecuteCountKey = migrateProjectExecuteCountKey
                    )
                } catch (ignored: Throwable) {
                    logger.warn("migrateProjectData doMigrateProjectDataTask fail|params:[$userId|$projectId]", ignored)
                    sendMigrateProcessDataFailMsg(projectId, userId, ignored.message)
                }
                logger.info("migrateProjectData end,params:[$userId|$projectId]")
            }
        } finally {
            // 从正在迁移的项目集合移除该项目
            redisOperation.removeSetMember(
                key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
                item = projectId
            )
            // 更新同时迁移的项目数量
            redisOperation.increment(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY, -1)
            // 解锁项目,允许用户发起新构建等操作
            redisOperation.removeSetMember(BkApiUtil.getApiAccessLimitProjectsKey(), projectId)
        }
        return true
    }

    private fun doMigrateProjectDataTask(
        migratingShardingDslContext: DSLContext,
        userId: String,
        projectId: String,
        dataTag: String?,
        cancelFlag: Boolean,
        sourceDataSourceName: String,
        routingRuleMap: Map<String, String>,
        projectExecuteCount: Int,
        migrateProjectExecuteCountKey: String
    ) {
        val clusterName = CommonUtils.getDbClusterName()
        val migratingShardingRoutingRuleKey = ShardingUtil.getMigratingShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = SystemModuleEnum.PROCESS.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = projectId
        )
        val migratingShardingRoutingRule = redisOperation.get(migratingShardingRoutingRuleKey)
        val shardingRoutingRule = routingRuleMap[migratingShardingRoutingRule]
        if (shardingRoutingRule.isNullOrBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        }
        val migrationLock = MigrationLock(redisOperation, projectId)
        // 把项目加入正在迁移项目集合中
        redisOperation.addSetValue(
            key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
            item = projectId
        )
        // 重试迁移需删除迁移库的数据以保证迁移接口的幂等性
        val deleteMigrationDataParam = DeleteMigrationDataParam(
            dslContext = migratingShardingDslContext,
            projectId = projectId,
            targetClusterName = clusterName,
            targetDataSourceName = shardingRoutingRule,
            migrationLock = migrationLock
        )
        processMigrationDataDeleteService.deleteProcessData(deleteMigrationDataParam)
        // 查询项目下流水线数量
        val pipelineNum = processDao.getPipelineNumByProjectId(dslContext, projectId)
        // 根据流水线数量计算线程数量
        val threadNum = if (pipelineNum < DEFAULT_THREAD_NUM) {
            pipelineNum
        } else {
            DEFAULT_THREAD_NUM
        }
        // 根据线程数量创建线程池
        val executor = Executors.newFixedThreadPool(threadNum)
        // 根据线程数量创建信号量
        val semaphore = Semaphore(threadNum)
        // 根据流水线数量创建计数器
        val doneSignal = CountDownLatch(pipelineNum)
        var minPipelineInfoId = processDao.getMinPipelineInfoIdByProjectId(dslContext, projectId)
        do {
            val pipelineIdList = processDao.getPipelineIdListByProjectId(
                dslContext = dslContext,
                projectId = projectId,
                minId = minPipelineInfoId,
                limit = DEFAULT_PAGE_SIZE.toLong()
            )?.map { it.getValue(0).toString() }
            if (!pipelineIdList.isNullOrEmpty()) {
                // 重置minId的值
                minPipelineInfoId = (processDao.getPipelineInfoByPipelineId(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineIdList[pipelineIdList.size - 1]
                )?.id ?: 0L) + 1
            }
            pipelineIdList?.forEach { pipelineId ->
                // 开启异步任务迁移项目下流水线数据
                executor.submit(
                    MigratePipelineDataTask(
                        migratePipelineDataParam = MigratePipelineDataParam(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            cancelFlag = cancelFlag,
                            semaphore = semaphore,
                            doneSignal = doneSignal,
                            dslContext = dslContext,
                            migratingShardingDslContext = migratingShardingDslContext,
                            processDao = processDao,
                            processDataMigrateDao = processDataMigrateDao
                        )
                    )
                )
            }
        } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)
        try {
            // 迁移与项目直接相关的数据
            doMigrationBus(migratingShardingDslContext, projectId)
        } catch (ignored: Throwable) {
            logger.warn("migrateProjectData doMigrationBus fail|params:[$userId|$projectId]", ignored)
            // 删除迁移库的数据
            processMigrationDataDeleteService.deleteProjectDirectlyRelData(
                dslContext = migratingShardingDslContext,
                projectId = projectId,
                targetClusterName = clusterName,
                targetDataSourceName = shardingRoutingRule,
                migrationLock = migrationLock
            )
            return
        }
        val historyShardingRoutingRule =
            client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.DB
            ).data
        try {
            // 等待所有任务执行完成
            doneSignal.await(migrationTimeout, TimeUnit.HOURS)
            logger.info("migrateProjectData all pipeline tasks have been completed|params:[$userId|$projectId]")
            // 执行迁移完成后的逻辑
            doAfterMigrationBus(
                userId = userId,
                projectId = projectId,
                sourceDataSourceName = sourceDataSourceName,
                migratingShardingRoutingRuleKey = migratingShardingRoutingRuleKey,
                migratingShardingRoutingRule = migratingShardingRoutingRule,
                shardingRoutingRule = shardingRoutingRule,
                pipelineNum = pipelineNum,
                migrationLock = migrationLock,
                dataTag = dataTag
            )
        } catch (ignored: Throwable) {
            val errorMsg = ignored.message
            logger.warn("migrateProjectData fail|params:[$userId|$projectId]|error=$errorMsg", ignored)
            // 执行迁移出错的逻辑
            doMigrationErrorBus(
                migratingShardingDslContext = migratingShardingDslContext,
                projectExecuteCount = projectExecuteCount,
                migrateProjectExecuteCountKey = migrateProjectExecuteCountKey,
                projectId = projectId,
                userId = userId,
                historyShardingRoutingRule = historyShardingRoutingRule,
                shardingRoutingRule = shardingRoutingRule,
                migrationLock = migrationLock,
                errorMsg = errorMsg
            )
        }
    }

    private fun doMigrationErrorBus(
        migratingShardingDslContext: DSLContext,
        projectExecuteCount: Int,
        migrateProjectExecuteCountKey: String,
        projectId: String,
        userId: String,
        migrationLock: MigrationLock,
        historyShardingRoutingRule: ShardingRoutingRule?,
        shardingRoutingRule: String?,
        errorMsg: String? = null
    ) {
        // 判断项目执行的次数是否是最新发起的，只有最新发起的才需要执行数据回滚逻辑
        val projectCurrentExecuteCount = projectExecuteCount + 1
        val projectLatestExecuteCount = redisOperation.get(migrateProjectExecuteCountKey)?.toInt()
        if (projectCurrentExecuteCount != projectLatestExecuteCount) {
            logger.warn(
                "migrateProjectData project:[$projectId] executeCount validate fail|" +
                    "projectCurrentExecuteCount:$projectCurrentExecuteCount|" +
                    "projectLatestExecuteCount:$projectLatestExecuteCount"
            )
            return
        }
        val clusterName = CommonUtils.getDbClusterName()
        try {
            // 删除迁移库的数据
            shardingRoutingRule?.let {
                val deleteMigrationDataParam = DeleteMigrationDataParam(
                    dslContext = migratingShardingDslContext,
                    projectId = projectId,
                    targetClusterName = clusterName,
                    targetDataSourceName = shardingRoutingRule,
                    migrationLock = migrationLock
                )
                processMigrationDataDeleteService.deleteProcessData(deleteMigrationDataParam)
            }
            // 把项目路由规则还原
            val updateShardingRoutingRule = historyShardingRoutingRule
                ?: ShardingRoutingRule(
                    clusterName = CommonUtils.getDbClusterName(),
                    moduleCode = SystemModuleEnum.PROCESS,
                    dataSourceName = DEFAULT_DATA_SOURCE_NAME,
                    type = ShardingRuleTypeEnum.DB,
                    routingName = projectId,
                    routingRule = DEFAULT_DATA_SOURCE_NAME
                )
            historyShardingRoutingRule?.let {
                client.get(ServiceShardingRoutingRuleResource::class).updateShardingRoutingRule(
                    userId = userId,
                    shardingRoutingRule = updateShardingRoutingRule
                )
            }
            // 判断微服务所有服务器的缓存是否已经全部更新成一致，不一致需要人工介入确认
            confirmAllServiceCacheIsUpdated(projectId)
        } catch (ignored: Throwable) {
            logger.warn("migrateProjectData project:[$projectId] restore data fail!", ignored)
            sendMigrateProcessDataFailMsg(projectId, userId, ignored.message)
            return
        }
        sendMigrateProcessDataFailMsg(projectId, userId, errorMsg)
    }

    private fun sendMigrateProcessDataFailMsg(projectId: String, userId: String, errorMsg: String?) {
        val titleParams = mapOf(KEY_PROJECT_ID to projectId)
        val bodyParams = mapOf(KEY_PROJECT_ID to projectId, FAIL_MSG to (errorMsg ?: ""))
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE,
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
                "migrateProjectData project:[$projectId] send msg" +
                    " template(MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE) fail!"
            )
        }
    }

    private fun doPreMigrationBus(
        projectId: String,
        sourceDataSourceName: String,
        dataTag: String? = null
    ): Triple<String, Int, Map<String, String>> {
        // 判断同时迁移的项目数量是否超过限制
        val migrationProjectCount = redisOperation.get(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY)?.toInt() ?: 0
        if (migrationProjectCount >= migrationMaxProjectCount) {
            throw ErrorCodeException(
                errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NUM_TOO_MANY,
                params = arrayOf(migrationMaxProjectCount.toString())
            )
        }
        // 判断项目是否超过规定的重试次数
        val migrateProjectExecuteCountKey = getMigrateProjectExecuteCountKey(projectId)
        val projectExecuteCount = redisOperation.get(migrateProjectExecuteCountKey)?.toInt() ?: 0
        if (projectExecuteCount >= RETRY_NUM) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INTERFACE_RETRY_NUM_EXCEEDED,
                params = arrayOf(RETRY_NUM.toString())
            )
        }
        // 锁定项目,不允许用户发起新构建等操作
        redisOperation.addSetValue(BkApiUtil.getApiAccessLimitProjectsKey(), projectId)
        // 为项目分配路由规则
        val routingRuleMap = assignShardingRoutingRule(projectId, sourceDataSourceName, dataTag)
        // 把同时迁移的项目数量存入redis中
        if (migrationProjectCount < 1) {
            redisOperation.setIfAbsent(
                key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY,
                value = "1",
                expired = false
            )
        } else {
            redisOperation.increment(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY, 1)
        }
        // 把项目数据迁移次数存入redis中
        if (projectExecuteCount < 1) {
            redisOperation.setIfAbsent(migrateProjectExecuteCountKey, "1", TimeUnit.HOURS.toSeconds(migrationTimeout))
        } else {
            redisOperation.increment(migrateProjectExecuteCountKey, 1)
        }
        return Triple(migrateProjectExecuteCountKey, projectExecuteCount, routingRuleMap)
    }

    fun getMigrateProjectExecuteCountKey(projectId: String): String {
        return "MIGRATE_PROJECT_PROCESS_DATA_EXECUTE_COUNT:$projectId"
    }

    private fun doMigrationBus(migratingShardingDslContext: DSLContext, projectId: String) {
        migrateAuditResourceData(migratingShardingDslContext, projectId)
        migratePipelineGroupData(migratingShardingDslContext, projectId)
        migratePipelineJobMutexGroupData(migratingShardingDslContext, projectId)
        migratePipelineLabelData(migratingShardingDslContext, projectId)
        migratePipelineViewData(migratingShardingDslContext, projectId)
        migratePipelineViewUserLastViewData(migratingShardingDslContext, projectId)
        migratePipelineViewUserSettingsData(migratingShardingDslContext, projectId)
        migrateProjectPipelineCallbackData(migratingShardingDslContext, projectId)
        migrateProjectPipelineCallbackHistoryData(migratingShardingDslContext, projectId)
        migrateTemplateData(migratingShardingDslContext, projectId)
        migratePipelineViewTopData(migratingShardingDslContext, projectId)
        migrateProjectPipelineTriggerEventData(migratingShardingDslContext, projectId)
    }

    private fun doAfterMigrationBus(
        userId: String,
        projectId: String,
        sourceDataSourceName: String,
        migratingShardingRoutingRuleKey: String,
        migratingShardingRoutingRule: String?,
        shardingRoutingRule: String,
        pipelineNum: Int,
        migrationLock: MigrationLock,
        dataTag: String?
    ) {
        val clusterName = CommonUtils.getDbClusterName()
        try {
            migrationLock.lock()
            logger.info("migrateProjectData doAfterMigrationBus get lock|params:[$userId|$projectId]")
            dslContext.transaction { t ->
                val context = DSL.using(t)
                if (migrationSourceDbDataDeleteFlag) {
                    // 开关打开则删除原库的数据
                    val deleteMigrationDataParam = DeleteMigrationDataParam(
                        dslContext = context,
                        projectId = projectId,
                        targetClusterName = clusterName,
                        targetDataSourceName = shardingRoutingRule,
                        broadcastTableDeleteFlag = !migrationProcessDbUnionClusterFlag
                    )
                    processMigrationDataDeleteService.deleteProcessData(deleteMigrationDataParam)
                }
                // 更新项目的路由规则
                updateShardingRoutingRule(
                    projectId = projectId,
                    userId = userId,
                    migratingShardingRoutingRuleKey = migratingShardingRoutingRuleKey,
                    migratingShardingRoutingRule = migratingShardingRoutingRule,
                    shardingRoutingRule = shardingRoutingRule
                )
            }
            // 保存项目迁移成功记录
            projectDataMigrateHistoryService.add(
                userId = userId,
                projectDataMigrateHistory = ProjectDataMigrateHistory(
                    id = UUIDUtil.generate(),
                    projectId = projectId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    sourceClusterName = clusterName,
                    sourceDataSourceName = sourceDataSourceName,
                    targetClusterName = clusterName,
                    targetDataSourceName = shardingRoutingRule,
                    targetDataTag = dataTag
                )
            )
        } finally {
            migrationLock.unlock()
        }
        // 删除项目执行次数记录
        redisOperation.delete(getMigrateProjectExecuteCountKey(projectId))
        // 发送迁移成功消息
        val titleParams = mapOf(KEY_PROJECT_ID to projectId)
        val bodyParams = mapOf(KEY_PROJECT_ID to projectId, KEY_PIPELINE_NUM to pipelineNum.toString())
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE,
            receivers = mutableSetOf(userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
        try {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.info(
                "migrateProjectData project:[$projectId] send msg" +
                    " template(MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE) fail!"
            )
        }
    }

    private fun updateShardingRoutingRule(
        projectId: String,
        userId: String,
        migratingShardingRoutingRuleKey: String,
        migratingShardingRoutingRule: String?,
        shardingRoutingRule: String?
    ) {
        if (migratingShardingRoutingRule != null && shardingRoutingRule != null) {
            // 清除缓存中项目的迁移DB路由规则
            redisOperation.delete(migratingShardingRoutingRuleKey)
            // 更新项目原来的路由规则
            val client = SpringContextUtil.getBean(Client::class.java)
            val updateResult = client.get(ServiceShardingRoutingRuleResource::class).updateShardingRoutingRule(
                userId = userId,
                shardingRoutingRule = ShardingRoutingRule(
                    clusterName = CommonUtils.getDbClusterName(),
                    moduleCode = SystemModuleEnum.PROCESS,
                    dataSourceName = shardingRoutingRule,
                    type = ShardingRuleTypeEnum.DB,
                    routingName = projectId,
                    routingRule = shardingRoutingRule
                )
            )
            if (updateResult.isNotOk()) {
                logger.warn("project[$projectId] updateShardingRoutingRule fail")
                throw ErrorCodeException(
                    errorCode = updateResult.status.toString(),
                    defaultMessage = updateResult.message
                )
            }
            // 睡眠一会儿等待服务器缓存更新
            Thread.sleep(DEFAULT_THREAD_SLEEP_TIMEOUT)
            // 判断微服务所有服务器的缓存是否已经全部更新完成
            confirmAllServiceCacheIsUpdated(projectId)
        }
    }

    private fun confirmAllServiceCacheIsUpdated(projectId: String) {
        val serviceNames = migrationProcessDbMicroServices.split(",")
        serviceNames.forEach { serviceName ->
            logger.info("service[$serviceName] confirmCacheIsUpdated start")
            val cacheKey = ShardingUtil.getShardingRoutingRuleKey(
                clusterName = CommonUtils.getDbClusterName(),
                moduleCode = SystemModuleEnum.PROCESS.name,
                ruleType = ShardingRuleTypeEnum.DB.name,
                routingName = projectId
            )
            val cacheUpdateFinishFlag = confirmCacheIsUpdated(serviceName, cacheKey, RETRY_NUM)
            logger.info("service[$serviceName] cacheUpdateFinishFlag:$cacheUpdateFinishFlag")
            if (!cacheUpdateFinishFlag) {
                // 服务器缓存更新失败，抛出错误提示
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_UPDATE_MICRO_SERVICE_LOCAL_RULE_CACHE_FAIL,
                    params = arrayOf(serviceName),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_UPDATE_MICRO_SERVICE_LOCAL_RULE_CACHE_FAIL,
                        params = arrayOf(serviceName)
                    )
                )
            }
        }
    }

    private fun confirmCacheIsUpdated(serviceName: String, cacheKey: String, retryNum: Int): Boolean {
        // 判断重试次数是否超限，如果超限就返回false
        if (retryNum < 1) {
            return false
        }
        // 获取当前微服务服务器IP列表
        val finalServiceName = BkServiceUtil.findServiceName(serviceName = serviceName)
        val serviceHostKey = BkServiceUtil.getServiceHostKey(finalServiceName)
        val serviceIps = redisOperation.getSetMembers(serviceHostKey)?.toMutableSet()
        val serviceRoutingRuleActionFinishKey = BkServiceUtil.getServiceRoutingRuleActionFinishKey(
            serviceName = finalServiceName,
            routingName = cacheKey,
            actionType = CrudEnum.UPDATE
        )
        val finishServiceIps = redisOperation.getSetMembers(serviceRoutingRuleActionFinishKey)
        // 判断所有服务器缓存是否已经更新成功
        finishServiceIps?.let { serviceIps?.removeAll(finishServiceIps) }
        logger.info(
            "confirmCacheIsUpdated service[$serviceName] cacheKey:$cacheKey retryNum:$retryNum " +
                "serviceIps:$serviceIps finishServiceIps:$finishServiceIps"
        )
        return if (serviceIps.isNullOrEmpty()) {
            true
        } else {
            confirmCacheIsUpdated(serviceName, cacheKey, retryNum - 1)
        }
    }

    private fun assignShardingRoutingRule(
        projectId: String,
        sourceDataSourceName: String,
        dataTag: String? = null
    ): Map<String, String> {
        val clusterName = CommonUtils.getDbClusterName()
        val moduleCode = SystemModuleEnum.PROCESS
        // 根据标签查找可用的数据源(需要排除项目原来分配的数据源)
        val dataSourceNames = dataSourceService.listByModule(
            clusterName = clusterName,
            moduleCode = moduleCode,
            fullFlag = false,
            dataTag = dataTag
        )?.map { it.dataSourceName }?.filter { it != sourceDataSourceName }
        if (dataSourceNames.isNullOrEmpty()) {
            logger.warn("[$clusterName]$moduleCode has no dataSource available")
            throw ErrorCodeException(errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NO_VALID_DB_ASSIGN)
        }
        val maxSizeIndex = dataSourceNames.size - 1
        val randomIndex = (0..maxSizeIndex).random()
        val routingRule = "${MIGRATING_DATA_SOURCE_NAME_PREFIX}$randomIndex"
        val key = ShardingUtil.getMigratingShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = SystemModuleEnum.PROCESS.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = projectId
        )
        // 把项目在迁移db集群的路由规则写入redis
        redisOperation.setIfAbsent(key, routingRule)
        return mapOf(routingRule to dataSourceNames[randomIndex])
    }

    private fun migrateAuditResourceData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val auditResourceRecords = processDataMigrateDao.getAuditResourceRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (auditResourceRecords.isNotEmpty()) {
                processDataMigrateDao.migrateAuditResourceData(migratingShardingDslContext, auditResourceRecords)
            }
            offset += MEDIUM_PAGE_SIZE
        } while (auditResourceRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migratePipelineGroupData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineGroupRecords = processDataMigrateDao.getPipelineGroupRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineGroupRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineGroupData(migratingShardingDslContext, pipelineGroupRecords)
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineGroupRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineJobMutexGroupData(migratingShardingDslContext: DSLContext, projectId: String) {
        val jobMutexGroupRecords = processDataMigrateDao.getPipelineJobMutexGroupRecords(
            dslContext = dslContext,
            projectId = projectId
        )
        if (jobMutexGroupRecords.isNotEmpty()) {
            processDataMigrateDao.migratePipelineJobMutexGroupData(migratingShardingDslContext, jobMutexGroupRecords)
        }
    }

    private fun migratePipelineLabelData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineLabelRecords = processDataMigrateDao.getPipelineLabelRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineLabelRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineLabelData(migratingShardingDslContext, pipelineLabelRecords)
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineLabelRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineViewData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineViewRecords = processDataMigrateDao.getPipelineViewRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineViewRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineViewData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineViewRecords = pipelineViewRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineViewRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineViewUserLastViewData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineViewUserLastViewRecords = processDataMigrateDao.getPipelineViewUserLastViewRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineViewUserLastViewRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineViewUserLastViewData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineViewUserLastViewRecords = pipelineViewUserLastViewRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineViewUserLastViewRecords.size == LONG_PAGE_SIZE)
    }

    private fun migratePipelineViewUserSettingsData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineViewUserSettingsRecords = processDataMigrateDao.getPipelineViewUserSettingsRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineViewUserSettingsRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineViewUserSettingsData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineViewUserSettingsRecords = pipelineViewUserSettingsRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineViewUserSettingsRecords.size == LONG_PAGE_SIZE)
    }

    private fun migrateProjectPipelineCallbackData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val projectPipelineCallbackRecords = processDataMigrateDao.getProjectPipelineCallbackRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (projectPipelineCallbackRecords.isNotEmpty()) {
                processDataMigrateDao.migrateProjectPipelineCallbackData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    projectPipelineCallbackRecords = projectPipelineCallbackRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (projectPipelineCallbackRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migrateProjectPipelineCallbackHistoryData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineCallbackHistoryRecords = processDataMigrateDao.getProjectPipelineCallbackHistoryRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (pipelineCallbackHistoryRecords.isNotEmpty()) {
                processDataMigrateDao.migrateProjectPipelineCallbackHistoryData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineCallbackHistoryRecords = pipelineCallbackHistoryRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (pipelineCallbackHistoryRecords.size == MEDIUM_PAGE_SIZE)
    }

    private fun migrateTemplateData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val templateRecords = processDataMigrateDao.getTemplateRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = SHORT_PAGE_SIZE,
                offset = offset
            )
            if (templateRecords.isNotEmpty()) {
                processDataMigrateDao.migrateTemplateData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    templateRecords = templateRecords
                )
            }
            offset += SHORT_PAGE_SIZE
        } while (templateRecords.size == SHORT_PAGE_SIZE)
    }

    private fun migratePipelineViewTopData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineViewTopRecords = processDataMigrateDao.getPipelineViewTopRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = LONG_PAGE_SIZE,
                offset = offset
            )
            if (pipelineViewTopRecords.isNotEmpty()) {
                processDataMigrateDao.migratePipelineViewTopData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineViewTopRecords = pipelineViewTopRecords
                )
            }
            offset += LONG_PAGE_SIZE
        } while (pipelineViewTopRecords.size == LONG_PAGE_SIZE)
    }

    private fun migrateProjectPipelineTriggerEventData(migratingShardingDslContext: DSLContext, projectId: String) {
        var offset = 0
        do {
            val pipelineTriggerEventRecords = processDataMigrateDao.getProjectPipelineTriggerEventRecords(
                dslContext = dslContext,
                projectId = projectId,
                limit = MEDIUM_PAGE_SIZE,
                offset = offset
            )
            if (pipelineTriggerEventRecords.isNotEmpty()) {
                processDataMigrateDao.migrateProjectPipelineTriggerEventData(
                    migratingShardingDslContext = migratingShardingDslContext,
                    pipelineTriggerEventRecords = pipelineTriggerEventRecords
                )
            }
            offset += MEDIUM_PAGE_SIZE
        } while (pipelineTriggerEventRecords.size == MEDIUM_PAGE_SIZE)
    }
}
