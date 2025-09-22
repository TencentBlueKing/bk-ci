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
import com.tencent.devops.common.web.utils.I18nUtil
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

    /**
     * 迁移执行主入口
     * 1. 准备迁移环境
     * 2. 执行数据迁移
     * 3. 完成迁移后处理
     * 4. 异常处理和资源清理
     */
    fun execute() {
        val projectId = config.projectId
        logger.info("Starting migration for project $projectId")
        try {
            // 1. 准备迁移环境
            prepareMigration()
            // 2. 执行数据迁移
            migrateProjectData()
            // 3. 完成迁移后处理
            completeMigration()
            logger.info("Migration completed successfully for project $projectId")
        } catch (ignored: Throwable) {
            // 迁移失败处理
            handleMigrationFailure(ignored)
            throw ignored
        } finally {
            // 资源清理
            cleanupResources()
        }
    }

    /**
     * 准备迁移环境
     * 1. 将项目标记为迁移中状态
     * 2. 清理目标数据库中的旧数据
     */
    private fun prepareMigration() {
        // 在Redis中标记项目为迁移中状态
        config.redisOperation.addSetValue(
            key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
            item = config.projectId
        )
        // 删除目标数据库中的旧数据
        deleteTargetData()
    }

    /**
     * 删除目标数据库中的现有数据
     * 防止重复数据导致迁移失败
     */
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

    /**
     * 项目数据迁移
     * 1. 先迁移流水线数据（多线程并行）
     * 2. 再迁移项目级数据（单线程顺序执行）
     */
    private fun migrateProjectData() {
        // 先启动耗时的多线程迁移流水线维度数据
        migratePipelineData()
        // 然后执行单线程迁移项目维度数据
        migrateProjectDirectData()
    }

    /**
     * 迁移项目级数据
     * 使用策略模式执行不同数据表的迁移
     */
    private fun migrateProjectDirectData() {
        val migrationContext = MigrationContext(
            dslContext = config.dslContext,
            migratingShardingDslContext = config.migratingShardingDslContext,
            projectId = config.projectId
        )
        // 遍历所有项目级数据迁移策略并执行
        config.migrationStrategyFactory.getProjectDataMigrationStrategies().forEach { strategy ->
            strategy.migrate(migrationContext)
        }
    }

    /**
     * 迁移流水线维度数据
     * 1. 分页查询流水线ID列表
     * 2. 使用线程池并行迁移每个流水线
     * 3. 使用CountDownLatch等待所有任务完成
     * 4. 添加超时机制防止永久阻塞
     */
    private fun migratePipelineData() {
        val projectId = config.projectId
        // 获取项目下流水线总数
        val pipelineNum = config.processDao.getPipelineNumByProjectId(config.dslContext, projectId)
        // 无流水线则终止流程
        if (pipelineNum <= 0) return
        // 动态计算线程池大小（不超过默认最大值）
        val threadNum = if (pipelineNum < DEFAULT_THREAD_NUM) {
            pipelineNum
        } else {
            DEFAULT_THREAD_NUM
        }
        // 创建线程池和同步工具
        val executor = Executors.newFixedThreadPool(threadNum)
        val semaphore = Semaphore(threadNum)
        val doneSignal = CountDownLatch(pipelineNum)
        val processDao = config.processDao
        val dslContext = config.dslContext
        try {
            var minPipelineInfoId = processDao.getMinPipelineInfoIdByProjectId(dslContext, projectId)
            do {
                // 分页查询流水线ID列表
                val pipelineIdList = processDao.getPipelineIdListByProjectId(
                    dslContext = dslContext,
                    projectId = projectId,
                    minId = minPipelineInfoId,
                    limit = DEFAULT_PAGE_SIZE.toLong()
                )?.map { it.getValue(0).toString() }

                // 提交每个流水线的迁移任务
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
                }
                // 更新分页起始ID（基于最后一条记录的ID）
                if (!pipelineIdList.isNullOrEmpty()) {
                    minPipelineInfoId = (processDao.getPipelineInfoByPipelineId(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineIdList.last()
                    )?.id ?: 0L) + 1
                }
            } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)

            // 等待所有任务完成（带超时机制）
            if (!doneSignal.await(config.migrationTimeout, TimeUnit.HOURS)) {
                logger.error("Pipeline migration timed out for project ${config.projectId}")
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_DATA_FAIL,
                    params = arrayOf(projectId),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_DATA_FAIL,
                        params = arrayOf(projectId)
                    )
                )
            }
        } finally {
            // 确保资源释放
            executor.shutdownNow()
            // 释放信号量资源
            releaseSemaphorePermits(semaphore, threadNum)
        }
    }

    private fun releaseSemaphorePermits(semaphore: Semaphore, permits: Int) {
        if (semaphore.availablePermits() < permits) {
            semaphore.release(permits - semaphore.availablePermits())
        }
    }

    /**
     * 迁移完成处理：
     * 1. 选择性删除源数据（配置决定）
     * 2. 更新分片路由规则
     * 3. 保存迁移历史记录
     * 4. 发送成功通知
     */
    private fun completeMigration() {
        // 根据配置决定是否删除源数据库数据
        if (config.migrationSourceDbDataDeleteFlag) {
            deleteSourceData()
        }
        // 更新路由规则
        updateShardingRules()
        // 保存迁移历史记录
        saveMigrationHistory()
        // 发送迁移成功通知
        sendSuccessNotification()
    }

    /**
     * 删除源数据库中的数据
     * （仅在配置开启时执行）
     */
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

    /**
     * 更新分片路由规则：
     * 1. 将项目路由指向新数据源
     * 2. 清理迁移临时路由
     * 3. 等待并验证所有微服务缓存更新
     */
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
        // 清理临时路由规则
        config.redisOperation.delete(migratingShardingRoutingRuleKey)
        // 睡眠一会儿等待服务器缓存更新
        Thread.sleep(DEFAULT_THREAD_SLEEP_TIMEOUT)
        // 判断微服务所有服务器的缓存是否已经全部更新完成
        confirmAllServiceCacheUpdated()
    }

    /**
     * 确认所有微服务的路由缓存已更新
     * 循环检查所有服务的实例是否完成更新
     */
    private fun confirmAllServiceCacheUpdated() {
        val serviceNames = config.migrationProcessDbMicroServices.split(",")
        serviceNames.forEach { serviceName ->
            logger.info("Verifying cache update for service [$serviceName]")
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

    /**
     * 递归检查单个服务的缓存更新状态
     * @param serviceName 服务名称
     * @param cacheKey 路由缓存key
     * @param retryNum 重试次数
     * @return Boolean 是否所有实例都更新完成
     */
    private fun confirmCacheUpdated(serviceName: String, cacheKey: String, retryNum: Int): Boolean {
        if (retryNum <= 0) return false
        val finalServiceName = BkServiceUtil.findServiceName(serviceName = serviceName)
        val serviceHostKey = BkServiceUtil.getServiceHostKey(finalServiceName)
        val redisOperation = config.redisOperation
        // 获取该服务的所有实例IP
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
        // 计算未更新的实例
        serviceIps.removeAll(finishServiceIps)
        return if (serviceIps.isEmpty()) true
        else confirmCacheUpdated(serviceName, cacheKey, retryNum - 1)
    }

    /**
     * 保存迁移历史记录
     * 记录源和目标数据源信息
     */
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

    /**
     * 发送迁移成功通知
     */
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

    /**
     * 迁移失败处理：
     * 1. 记录错误日志
     * 2. 执行回滚操作
     * 3. 发送失败通知
     */
    private fun handleMigrationFailure(ignored: Throwable) {
        logger.error("Migration failed for project ${config.projectId}", ignored)
        // 回滚已迁移的数据
        rollbackMigration()
        // 发送迁移失败通知
        sendFailureNotification(ignored.message)
    }

    /**
     * 回滚操作：
     * 1. 删除目标数据库中的迁移数据
     * 2. 恢复原始路由规则
     * 3. 验证服务缓存回滚
     */
    private fun rollbackMigration() {
        val projectId = config.projectId
        try {
            val targetDataSourceName = getTargetDataSourceName()
            // 删除目标数据库中的迁移数据
            val deleteDataParam = DeleteDataParam(
                dslContext = config.migratingShardingDslContext,
                projectId = projectId,
                lock = migrationLock,
                targetClusterName = clusterName,
                targetDataSourceName = targetDataSourceName
            )
            config.processDataDeleteService.deleteProcessData(deleteDataParam)
            val client = config.client
            // 恢复原始路由规则
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
            // 验证服务缓存是否已回滚
            confirmAllServiceCacheUpdated()
        } catch (ignored: Throwable) {
            logger.warn("Rollback failed for project $projectId", ignored)
        }
    }

    /**
     * 发送迁移失败通知
     */
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

    /**
     * 资源清理：
     * 1. 移除迁移中状态
     * 2. 更新并发计数
     * 3. 解除项目锁定
     * 4. 清理执行计数
     */
    private fun cleanupResources() {
        val redisOperation = config.redisOperation
        val projectId = config.projectId
        // 从迁移中项目集合移除该项目
        redisOperation.removeSetMember(
            key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
            item = projectId
        )
        // 减少全局迁移项目计数
        redisOperation.increment(
            key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY,
            incr = -1
        )
        // 解除项目的API访问限制
        redisOperation.removeSetMember(
            key = BkApiUtil.getApiAccessLimitProjectsKey(),
            item = projectId
        )
        // 清理项目执行计数
        redisOperation.delete(getMigrateProjectExecuteCountKey(projectId))
    }

    private fun getMigrateProjectExecuteCountKey(projectId: String): String {
        return "MIGRATE_PROJECT_PROCESS_DATA_EXECUTE_COUNT:$projectId"
    }

    /**
     * 获取目标数据源名称
     * 从预迁移结果中提取路由规则映射
     */
    private fun getTargetDataSourceName(): String {
        val preMigrationResult = config.preMigrationResult
        val routingRule = preMigrationResult.routingRuleMap.keys.firstOrNull()
            ?: throw IllegalStateException("No routing rule found")
        return preMigrationResult.routingRuleMap[routingRule]
            ?: throw IllegalStateException("No data source mapped to routing rule")
    }
}
