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
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.MIGRATING_DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.MIGRATING_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.BkApiUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataMigrateDao
import com.tencent.devops.misc.factory.MigrationStrategyFactory
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.MigrationExecutorConfig
import com.tencent.devops.misc.pojo.process.PreMigrationResult
import com.tencent.devops.misc.service.project.DataSourceService
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.misc.utils.MigrationExecutor
import com.tencent.devops.misc.utils.MiscUtils
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import jakarta.annotation.PostConstruct
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList")
@Service
class ProcessDataMigrateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val processDao: ProcessDao,
    processDataMigrateDao: ProcessDataMigrateDao,
    private val processDataDeleteService: ProcessDataDeleteService,
    private val dataSourceService: DataSourceService,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessDataMigrateService::class.java)
        private const val DEFAULT_MIGRATION_TIMEOUT = 2L
        private const val DEFAULT_MIGRATION_MAX_PROJECT_COUNT = 5
        private const val RETRY_NUM = 3
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

    // 策略工厂
    private val migrationStrategyFactory = MigrationStrategyFactory(processDataMigrateDao)

    @PostConstruct
    fun init() {
        // 启动的时候重置redis中存储的同时迁移的项目数量，防止因为服务异常停了造成程序执行出错
        redisOperation.setIfAbsent(
            key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY,
            value = "0",
            expired = false
        )
    }

    /**
     * 迁移项目数据的主入口方法
     * @param userId 执行迁移的用户ID
     * @param projectId 要迁移的项目ID
     * @param cancelFlag 是否取消正在构建的流水线标志
     * @param dataTag 数据标签（用于迁移数据至特定数据源）
     * @return Boolean 是否成功启动迁移
     */
    fun migrateProjectData(
        userId: String,
        projectId: String,
        cancelFlag: Boolean = false,
        dataTag: String? = null
    ): Boolean {
        // 获取迁移专用的JOOQ DSL上下文
        val migratingShardingDslContext = try {
            SpringContextUtil.getBean(DSLContext::class.java, MIGRATING_SHARDING_DSL_CONTEXT)
        } catch (ignored: Throwable) {
            logger.warn("migratingShardingDslContext is not exist", ignored)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR,
                defaultMessage = "migratingShardingDslContext is not exist"
            )
        }

        // 获取项目当前的数据源名称
        val sourceDataSourceName = getSourceDataSourceName(projectId)
        // 执行迁移前的准备工作
        val preMigrationResult = doPreMigration(projectId, sourceDataSourceName, dataTag)
        // 异步执行迁移任务
        val executor = Executors.newFixedThreadPool(1)

        executor.submit {
            try {
                val config = MigrationExecutorConfig(
                    migratingShardingDslContext = migratingShardingDslContext,
                    userId = userId,
                    projectId = projectId,
                    dataTag = dataTag,
                    cancelFlag = cancelFlag,
                    sourceDataSourceName = sourceDataSourceName,
                    preMigrationResult = preMigrationResult,
                    dslContext = dslContext,
                    processDao = processDao,
                    processDataDeleteService = processDataDeleteService,
                    redisOperation = redisOperation,
                    client = client,
                    migrationStrategyFactory = migrationStrategyFactory,
                    projectDataMigrateHistoryService = projectDataMigrateHistoryService,
                    migrationSourceDbDataDeleteFlag = migrationSourceDbDataDeleteFlag,
                    migrationProcessDbUnionClusterFlag = migrationProcessDbUnionClusterFlag,
                    migrationTimeout = migrationTimeout,
                    migrationProcessDbMicroServices = migrationProcessDbMicroServices
                )
                // 执行迁移任务
                MigrationExecutor(config).execute()
            } catch (ignored: Throwable) {
                logger.error("Migration failed for project $projectId", ignored)
            } finally {
                // 确保关闭线程池
                executor.shutdown()
            }
        }
        return true
    }

    /**
     * 获取项目当前的数据源名称
     * @param projectId 项目ID
     * @return String 数据源名称
     */
    private fun getSourceDataSourceName(projectId: String): String {
        return client.get(ServiceShardingRoutingRuleResource::class)
            .getShardingRoutingRuleByName(
                routingName = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                ruleType = ShardingRuleTypeEnum.DB
            ).data?.dataSourceName ?: throw ErrorCodeException(
            errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NO_VALID_DB_ASSIGN,
            params = arrayOf(projectId)
        )
    }

    /**
     * 执行迁移前的准备工作
     * 1. 验证迁移状态
     * 2. 锁定项目
     * 3. 分配分片路由规则
     * 4. 更新迁移计数器
     * @return PreMigrationResult 预迁移结果
     */
    private fun doPreMigration(
        projectId: String,
        sourceDataSourceName: String,
        dataTag: String? = null
    ): PreMigrationResult {
        // 验证项目是否可迁移
        validateMigrationState(projectId)
        // 锁定项目防止产生新的数据
        lockProject(projectId)
        try {
            // 分配路由规则
            val routingRuleMap = assignShardingRoutingRule(projectId, sourceDataSourceName, dataTag)
            // 更新迁移计数器
            updateMigrationCounters(projectId)
            return PreMigrationResult(routingRuleMap)
        } catch (ignored: Throwable) {
            logger.error("doPreMigration execute bus failed for project $projectId", ignored)
            // 解锁项目
            unlockProject(projectId)
            throw ignored
        }
    }

    /**
     * 验证项目迁移状态
     * 检查项目是否正在迁移、是否超过最大并发迁移数、是否超过重试次数
     */
    private fun validateMigrationState(projectId: String) {
        // 检查项目是否已在迁移中
        if (redisOperation.isMember(
                key = MiscUtils.getMigratingProjectsRedisKey(SystemModuleEnum.PROCESS.name),
                item = projectId
            )
        ) {
            throw ErrorCodeException(
                errorCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                params = arrayOf(projectId)
            )
        }
        // 检查当前迁移项目数是否超过最大值
        val migrationProjectCount = redisOperation.get(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY)?.toInt() ?: 0
        if (migrationProjectCount >= migrationMaxProjectCount) {
            throw ErrorCodeException(
                errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NUM_TOO_MANY,
                params = arrayOf(migrationMaxProjectCount.toString())
            )
        }
        // 检查项目迁移重试次数是否超过限制
        val migrateProjectExecuteCountKey = getMigrateProjectExecuteCountKey(projectId)
        val projectExecuteCount = redisOperation.get(migrateProjectExecuteCountKey)?.toInt() ?: 0
        if (projectExecuteCount >= RETRY_NUM) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INTERFACE_RETRY_NUM_EXCEEDED,
                params = arrayOf(RETRY_NUM.toString())
            )
        }
    }

    /**
     * 锁定项目
     * 将项目添加到API访问限制列表，防止在迁移过程中被操作
     */
    private fun lockProject(projectId: String) {
        redisOperation.addSetValue(BkApiUtil.getApiAccessLimitProjectsKey(), projectId)
    }

    /**
     * 解锁项目
     * 将项目移除API访问限制列表
     */
    private fun unlockProject(projectId: String) {
        redisOperation.removeSetMember(
            key = BkApiUtil.getApiAccessLimitProjectsKey(),
            item = projectId
        )
    }

    /**
     * 更新迁移计数器
     * 1. 增加全局迁移项目计数
     * 2. 增加项目迁移尝试次数计数
     */
    private fun updateMigrationCounters(projectId: String) {
        // 更新全局迁移项目计数
        val migrationProjectCount = redisOperation.get(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY)?.toInt() ?: 0
        if (migrationProjectCount < 1) {
            redisOperation.set(
                key = MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY,
                value = "1",
                expired = false
            )
        } else {
            redisOperation.increment(MIGRATE_PROCESS_PROJECT_DATA_PROJECT_COUNT_KEY, 1)
        }
        // 更新项目迁移尝试次数
        val migrateProjectExecuteCountKey = getMigrateProjectExecuteCountKey(projectId)
        val projectExecuteCount = redisOperation.get(migrateProjectExecuteCountKey)?.toInt() ?: 0
        if (projectExecuteCount < 1) {
            redisOperation.set(
                key = migrateProjectExecuteCountKey,
                value = "1",
                expiredInSecond = TimeUnit.HOURS.toSeconds(migrationTimeout)
            )
        } else {
            redisOperation.increment(migrateProjectExecuteCountKey, 1)
        }
    }

    /**
     * 获取项目迁移尝试次数的Redis key
     */
    fun getMigrateProjectExecuteCountKey(projectId: String): String {
        return "MIGRATE_PROJECT_PROCESS_DATA_EXECUTE_COUNT:$projectId"
    }

    /**
     * 为项目分配分片路由规则
     * 1. 获取可用数据源列表
     * 2. 随机选择一个目标数据源
     * 3. 在Redis中设置迁移路由规则
     * @return Map<String, String> 路由规则映射（路由规则->数据源名称）
     */
    private fun assignShardingRoutingRule(
        projectId: String,
        sourceDataSourceName: String,
        dataTag: String? = null
    ): Map<String, String> {
        val clusterName = CommonUtils.getDbClusterName()
        val moduleCode = SystemModuleEnum.PROCESS
        // 获取可用数据源（排除当前数据源）
        val dataSourceNames = dataSourceService.listByModule(
            clusterName = clusterName,
            moduleCode = moduleCode,
            fullFlag = false,
            dataTag = dataTag
        )?.map { it.dataSourceName }?.filter { it != sourceDataSourceName }
            ?: throw ErrorCodeException(errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NO_VALID_DB_ASSIGN)
        // 检查是否有可用数据源
        if (dataSourceNames.isEmpty()) {
            throw ErrorCodeException(errorCode = MiscMessageCode.ERROR_MIGRATING_PROJECT_NO_VALID_DB_ASSIGN)
        }
        // 随机选择一个目标数据源
        val randomIndex = dataSourceNames.indices.random()
        val routingRule = "${MIGRATING_DATA_SOURCE_NAME_PREFIX}$randomIndex"
        // 在Redis中设置迁移路由规则
        val key = ShardingUtil.getMigratingShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = SystemModuleEnum.PROCESS.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = projectId
        )
        redisOperation.set(key, routingRule)
        return mapOf(routingRule to dataSourceNames[randomIndex])
    }
}
