package com.tencent.devops.misc.utils

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
import com.tencent.devops.common.db.pojo.DEFAULT_DATA_SOURCE_NAME
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.misc.lock.MigrationLock
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.DeleteDataParam
import com.tencent.devops.misc.pojo.process.MigratePipelineDataParam
import com.tencent.devops.misc.pojo.process.MigrationContext
import com.tencent.devops.misc.pojo.process.MigrationExecutorConfig
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.task.PipelineMigrationTask
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class MigrationExecutor(private val config: MigrationExecutorConfig) {
    private val logger = LoggerFactory.getLogger(MigrationExecutor::class.java)
    private val clusterName = CommonUtils.getDbClusterName()
    private val migrationLock = MigrationLock(config.redisOperation, config.projectId)

    companion object {
        private const val DEFAULT_THREAD_NUM = 10
        private const val DEFAULT_PAGE_SIZE = 20
        private const val DEFAULT_THREAD_SLEEP_TIMEOUT = 5000L
        private const val RETRY_NUM = 3
        private const val MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE = "MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE"
        private const val MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE =
            "MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE"
        private const val MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY = "MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT"
    }

    fun execute() {
        val projectId = config.projectId
        logger.info("Starting migration for project $projectId")
        try {
            prepareMigration()
            migrateProjectData()
            completeMigration()
            logger.info("Migration completed successfully for project $projectId")
        } catch (ignored: Throwable) {
            handleMigrationFailure(ignored)
            throw ignored
        } finally {
            cleanupResources()
        }
    }

    private fun prepareMigration() {
        config.redisOperation.addSetValue(
            key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
            item = config.projectId
        )
        deleteTargetData()
    }

    private fun deleteTargetData() {
        val targetDataSourceName = getTargetDataSourceName()
        val deleteDataParam = DeleteDataParam(
            dslContext = config.migratingShardingDslContext,
            projectId = config.projectId,
            lock = migrationLock,
            targetClusterName = clusterName,
            targetDataSourceName = targetDataSourceName
        )
        config.processDataDeleteService.deleteProcessData(deleteDataParam)
    }

    private fun migrateProjectData() {
        migrateProjectDirectData()
        migratePipelineData()
    }

    private fun migrateProjectDirectData() {
        val migrationContext = MigrationContext(
            dslContext = config.dslContext,
            migratingShardingDslContext = config.migratingShardingDslContext,
            projectId = config.projectId
        )
        config.migrationStrategyFactory.getProjectMigrationStrategies().forEach { strategy ->
            strategy.migrate(migrationContext)
        }
    }

    private fun migratePipelineData() {
        val projectId= config.projectId
        val pipelineNum = config.processDao.getPipelineNumByProjectId(config.dslContext, projectId)
        if (pipelineNum <= 0) return

        val threadNum = if (pipelineNum < DEFAULT_THREAD_NUM) {
            pipelineNum
        } else {
            DEFAULT_THREAD_NUM
        }

        val executor = Executors.newFixedThreadPool(threadNum)
        val semaphore = Semaphore(threadNum)
        val doneSignal = CountDownLatch(pipelineNum)
        val processDao = config.processDao
        val dslContext = config.dslContext
        try {
            var minPipelineInfoId = processDao.getMinPipelineInfoIdByProjectId(dslContext, projectId)
            do {
                val pipelineIdList = processDao.getPipelineIdListByProjectId(
                    dslContext = dslContext,
                    projectId = projectId,
                    minId = minPipelineInfoId,
                    limit = DEFAULT_PAGE_SIZE.toLong()
                )?.map { it.getValue(0).toString() }

                pipelineIdList?.forEach { pipelineId ->
                    executor.submit(
                        PipelineMigrationTask(
                            migratePipelineDataParam = MigratePipelineDataParam(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                cancelFlag = config.cancelFlag,
                                semaphore = semaphore,
                                doneSignal = doneSignal,
                                dslContext = dslContext,
                                migratingShardingDslContext = config.migratingShardingDslContext,
                                processDao = processDao,
                                migrationStrategyFactory = config.migrationStrategyFactory
                            )
                        )
                    )
                    minPipelineInfoId = updateMinPipelineId(pipelineIdList, minPipelineInfoId)
                }
            } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)

            // 添加超时机制防止永久阻塞
            if (!doneSignal.await(config.migrationTimeout, TimeUnit.HOURS)) {
                logger.error("Pipeline migration timed out for project ${config.projectId}")
            }
        } finally {
            // 确保资源释放
            executor.shutdownNow()
            releaseSemaphorePermits(semaphore, threadNum)
        }
    }

    private fun releaseSemaphorePermits(semaphore: Semaphore, permits: Int) {
        if (semaphore.availablePermits() < permits) {
            semaphore.release(permits - semaphore.availablePermits())
        }
    }

    private fun updateMinPipelineId(pipelineIdList: List<String>, currentMinId: Long): Long {
        return pipelineIdList.lastOrNull()?.let { lastPipelineId ->
            (config.processDao.getPipelineInfoByPipelineId(
                dslContext = config.dslContext,
                projectId = config.projectId,
                pipelineId = lastPipelineId
            )?.id ?: 0L) + 1
        } ?: currentMinId
    }

    private fun completeMigration() {
        if (config.migrationSourceDbDataDeleteFlag) {
            deleteSourceData()
        }
        updateShardingRules()
        saveMigrationHistory()
        sendSuccessNotification()
    }

    private fun deleteSourceData() {
        val targetDataSourceName = getTargetDataSourceName()
        val deleteDataParam = DeleteDataParam(
            dslContext = config.dslContext,
            projectId = config.projectId,
            broadcastTableDeleteFlag = !config.migrationProcessDbUnionClusterFlag,
            targetClusterName = clusterName,
            targetDataSourceName = targetDataSourceName
        )
        config.processDataDeleteService.deleteProcessData(deleteDataParam)
    }

    private fun updateShardingRules() {
        val projectId = config.projectId
        val migratingShardingRoutingRuleKey = ShardingUtil.getMigratingShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = SystemModuleEnum.PROCESS.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = projectId
        )
        val targetDataSourceName = getTargetDataSourceName()
        // 更新项目原来的路由规则
        config.client.get(ServiceShardingRoutingRuleResource::class).updateShardingRoutingRule(
            userId = config.userId,
            shardingRoutingRule = ShardingRoutingRule(
                clusterName = clusterName,
                moduleCode = SystemModuleEnum.PROCESS,
                dataSourceName = targetDataSourceName,
                type = ShardingRuleTypeEnum.DB,
                routingName = projectId,
                routingRule = targetDataSourceName
            )
        )
        // 清除缓存中项目的迁移DB路由规则
        config.redisOperation.delete(migratingShardingRoutingRuleKey)
        // 睡眠一会儿等待服务器缓存更新
        Thread.sleep(DEFAULT_THREAD_SLEEP_TIMEOUT)
        // 判断微服务所有服务器的缓存是否已经全部更新完成
        confirmAllServiceCacheUpdated()
    }

    private fun confirmAllServiceCacheUpdated() {
        val serviceNames = config.migrationProcessDbMicroServices.split(",")
        serviceNames.forEach { serviceName ->
            logger.info("service[$serviceName] confirmCacheIsUpdated start")
            val cacheKey = ShardingUtil.getShardingRoutingRuleKey(
                clusterName = clusterName,
                moduleCode = SystemModuleEnum.PROCESS.name,
                ruleType = ShardingRuleTypeEnum.DB.name,
                routingName = config.projectId
            )
            val cacheUpdateFinishFlag = confirmCacheUpdated(serviceName, cacheKey, RETRY_NUM)
            logger.info("service[$serviceName] cacheUpdateFinishFlag:$cacheUpdateFinishFlag")
            if (!cacheUpdateFinishFlag) {
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_UPDATE_MICRO_SERVICE_LOCAL_RULE_CACHE_FAIL,
                    params = arrayOf(serviceName)
                )
            }
        }
    }

    private fun confirmCacheUpdated(serviceName: String, cacheKey: String, retryNum: Int): Boolean {
        if (retryNum <= 0) return false
        
        val finalServiceName = BkServiceUtil.findServiceName(serviceName = serviceName)
        val serviceHostKey = BkServiceUtil.getServiceHostKey(finalServiceName)
        val redisOperation = config.redisOperation
        val serviceIps = redisOperation.getSetMembers(serviceHostKey)?.toMutableSet() ?: mutableSetOf()
        val serviceRoutingRuleActionFinishKey = BkServiceUtil.getServiceRoutingRuleActionFinishKey(
            serviceName = finalServiceName,
            routingName = cacheKey,
            actionType = CrudEnum.UPDATE
        )
        // 判断所有服务器缓存是否已经更新成功
        val finishServiceIps = redisOperation.getSetMembers(serviceRoutingRuleActionFinishKey) ?: setOf()
        logger.info(
            "confirmCacheIsUpdated service[$serviceName] cacheKey:$cacheKey retryNum:$retryNum " +
                    "serviceIps:$serviceIps finishServiceIps:$finishServiceIps"
        )
        serviceIps.removeAll(finishServiceIps)
        return if (serviceIps.isEmpty()) true
        else confirmCacheUpdated(serviceName, cacheKey, retryNum - 1)
    }

    private fun saveMigrationHistory() {
        config.projectDataMigrateHistoryService.add(
            userId = config.userId,
            projectDataMigrateHistory = ProjectDataMigrateHistory(
                id = UUIDUtil.generate(),
                projectId = config.projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                sourceClusterName = clusterName,
                sourceDataSourceName = config.sourceDataSourceName,
                targetClusterName = clusterName,
                targetDataSourceName = getTargetDataSourceName(),
                targetDataTag = config.dataTag
            )
        )
    }

    private fun sendSuccessNotification() {
        val projectId = config.projectId
        val pipelineNum = config.processDao.getPipelineNumByProjectId(config.dslContext, projectId)
        val titleParams = mapOf(KEY_PROJECT_ID to projectId)
        val bodyParams = mapOf(KEY_PROJECT_ID to projectId, KEY_PIPELINE_NUM to pipelineNum.toString())
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE,
            receivers = mutableSetOf(config.userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
        try {
            config.client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.info(
                "migrateProjectData project:[$projectId] send msg" +
                        " template(MIGRATE_PROCESS_PROJECT_DATA_SUCCESS_TEMPLATE) fail!"
            )
        }
    }

    private fun handleMigrationFailure(ignored: Throwable) {
        logger.warn("Migration failed for project ${config.projectId}", ignored)
        rollbackMigration()
        sendFailureNotification(ignored.message)
    }

    private fun rollbackMigration() {
        val projectId = config.projectId
        try {
            val targetDataSourceName = getTargetDataSourceName()
            val deleteDataParam = DeleteDataParam(
                dslContext = config.migratingShardingDslContext,
                projectId = projectId,
                lock = migrationLock,
                targetClusterName = clusterName,
                targetDataSourceName = targetDataSourceName
            )
            config.processDataDeleteService.deleteProcessData(deleteDataParam)
            val client = config.client
            val historyShardingRoutingRule = client.get(ServiceShardingRoutingRuleResource::class)
                .getShardingRoutingRuleByName(
                    routingName = projectId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    ruleType = ShardingRuleTypeEnum.DB
                ).data ?: ShardingRoutingRule(
                    clusterName = clusterName,
                    moduleCode = SystemModuleEnum.PROCESS,
                    dataSourceName = DEFAULT_DATA_SOURCE_NAME,
                    type = ShardingRuleTypeEnum.DB,
                    routingName = projectId,
                    routingRule = DEFAULT_DATA_SOURCE_NAME
                )

            client.get(ServiceShardingRoutingRuleResource::class).updateShardingRoutingRule(
                userId = config.userId,
                shardingRoutingRule = historyShardingRoutingRule
            )

            confirmAllServiceCacheUpdated()
        } catch (ignored: Throwable) {
            logger.warn("Rollback failed for project $projectId", ignored)
        }
    }

    private fun sendFailureNotification(errorMsg: String?) {
        val projectId = config.projectId
        val titleParams = mapOf(KEY_PROJECT_ID to projectId)
        val bodyParams = mapOf(KEY_PROJECT_ID to projectId, FAIL_MSG to (errorMsg ?: ""))
        val request = SendNotifyMessageTemplateRequest(
            templateCode = MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE,
            receivers = mutableSetOf(config.userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
        try {
            // 发送迁移失败消息
            config.client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.warn(
                "migrateProjectData project:[$projectId] send msg" +
                        " template(MIGRATE_PROCESS_PROJECT_DATA_FAIL_TEMPLATE) fail!"
            )
        }
    }

    private fun cleanupResources() {
        val redisOperation = config.redisOperation
        val projectId = config.projectId
        redisOperation.removeSetMember(
            key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
            item = projectId
        )
        redisOperation.increment(
            key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY,
            incr = -1
        )
        redisOperation.removeSetMember(
            key = BkApiUtil.getApiAccessLimitProjectsKey(),
            item = projectId
        )
        redisOperation.delete(getMigrateProjectExecuteCountKey(projectId))
    }

    private fun getMigrateProjectExecuteCountKey(projectId: String): String {
        return "MIGRATE_PROJECT_PROCESS_DATA_EXECUTE_COUNT:$projectId"
    }

    private fun getTargetDataSourceName(): String {
        val preMigrationResult = config.preMigrationResult
        val routingRule = preMigrationResult.routingRuleMap.keys.firstOrNull()
            ?: throw IllegalStateException("No routing rule found")
        return preMigrationResult.routingRuleMap[routingRule] 
            ?: throw IllegalStateException("No data source mapped to routing rule")
    }
}