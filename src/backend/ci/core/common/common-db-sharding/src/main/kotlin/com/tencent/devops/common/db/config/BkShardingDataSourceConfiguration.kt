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

package com.tencent.devops.common.db.config

import com.mysql.cj.jdbc.Driver
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.db.pojo.ARCHIVE_DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.BindingTableGroupConfig
import com.tencent.devops.common.db.pojo.DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.DataSourceConfig
import com.tencent.devops.common.db.pojo.DataSourceProperties
import com.tencent.devops.common.db.pojo.DatabaseShardingStrategyEnum
import com.tencent.devops.common.db.pojo.MIGRATING_DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.TableRuleConfig
import com.tencent.devops.common.db.pojo.TableShardingStrategyEnum
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import java.util.Properties
import javax.sql.DataSource
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.apache.shardingsphere.broadcast.api.config.BroadcastRuleConfiguration
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableReferenceRuleConfiguration
import org.apache.shardingsphere.single.api.config.SingleRuleConfiguration

@Suppress("LongParameterList", "MagicNumber", "ComplexMethod")
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(DataSourceAutoConfiguration::class, JooqAutoConfiguration::class)
@EnableConfigurationProperties(DataSourceProperties::class)
@EnableTransactionManagement
class BkShardingDataSourceConfiguration {

    companion object {
        private const val DB_SHARDING_ALGORITHM_NAME = "databaseShardingAlgorithm"
        private const val TABLE_SHARDING_ALGORITHM_NAME = "tableShardingAlgorithm"
        private const val STRATEGY = "strategy"
        private const val STANDARD = "STANDARD"
        private const val ALGORITHM_CLASS_NAME = "algorithmClassName"
        private const val CLASS_BASED = "CLASS_BASED"
        private val logger = LoggerFactory.getLogger(BkShardingDataSourceConfiguration::class.java)
    }

    @Value("\${sharding.log.switch:false}")
    private val shardingLogSwitch: Boolean = false

    @Value("\${sharding.databaseShardingStrategy.algorithmClassName:#{null}}")
    private val databaseAlgorithmClassName: String? = null

    @Value("\${sharding.databaseShardingStrategy.migratingAlgorithmClassName:#{null}}")
    private val migratingDatabaseAlgorithmClassName: String? = null

    @Value("\${sharding.databaseShardingStrategy.archiveAlgorithmClassName:#{null}}")
    private val archiveDatabaseAlgorithmClassName: String? = null

    @Value("\${sharding.databaseShardingStrategy.shardingField:#{null}}")
    private val databaseShardingField: String? = null

    @Value("\${sharding.tableShardingStrategy.algorithmClassName:#{null}}")
    private val tableAlgorithmClassName: String? = null

    @Value("\${sharding.tableShardingStrategy.migratingAlgorithmClassName:#{null}}")
    private val migratingTableAlgorithmClassName: String? = null

    @Value("\${sharding.tableShardingStrategy.archiveAlgorithmClassName:#{null}}")
    private val archiveTableAlgorithmClassName: String? = null

    @Value("\${sharding.tableShardingStrategy.shardingField:#{null}}")
    private val tableShardingField: String? = null

    @Value("\${spring.datasource.minimumIdle:#{1}}")
    private val datasourceMinimumIdle: Int = 1

    @Value("\${spring.datasource.maximumPoolSize:#{50}}")
    private val datasourceMaximumPoolSize: Int = 50

    @Value("\${spring.datasource.idleTimeout:#{60000}}")
    private val datasourceIdleTimeout: Long = 60000

    @Value("\${sharding.tableShardingStrategy.defaultShardingNum:#{5}}")
    private val defaultTableShardingNum: Int = 5

    @Value("\${spring.datasource.connectionTestQuery:select 1;}")
    private lateinit var dataSourceConnectionTestQuery: String

    private fun dataSourceMap(
        dataSourcePrefixName: String,
        dataSourceConfigs: List<DataSourceConfig>,
        registry: MeterRegistry
    ): Map<String, DataSource> {
        val dataSourceMap: MutableMap<String, DataSource> = mutableMapOf()
        // 根据配置文件中的数据源配置项列表动态生成数据源集合
        dataSourceConfigs.forEach { dataSourceConfig ->
            val dataSourceName = "$dataSourcePrefixName${dataSourceConfig.index}"
            dataSourceMap[dataSourceName] = createHikariDataSource(
                datasourcePoolName = dataSourceName,
                datasourceUrl = dataSourceConfig.url,
                datasourceUsername = dataSourceConfig.username,
                datasourcePassword = dataSourceConfig.password,
                datasourceInitSql = dataSourceConfig.initSql,
                datasourceLeakDetectionThreshold = dataSourceConfig.leakDetectionThreshold,
                metricsRegistry = registry
            )
        }
        return dataSourceMap
    }

    private fun createHikariDataSource(
        datasourcePoolName: String,
        datasourceUrl: String,
        datasourceUsername: String,
        datasourcePassword: String,
        datasourceInitSql: String?,
        datasourceLeakDetectionThreshold: Long,
        metricsRegistry: MeterRegistry
    ): HikariDataSource {
        return HikariDataSource().apply {
            poolName = datasourcePoolName
            jdbcUrl = datasourceUrl
            username = datasourceUsername
            password = datasourcePassword
            driverClassName = Driver::class.java.name
            minimumIdle = datasourceMinimumIdle
            maximumPoolSize = datasourceMaximumPoolSize
            idleTimeout = datasourceIdleTimeout
            connectionInitSql = datasourceInitSql
            leakDetectionThreshold = datasourceLeakDetectionThreshold
            metricsTrackerFactory = MicrometerMetricsTrackerFactory(metricsRegistry)
            connectionTestQuery = dataSourceConnectionTestQuery
        }
    }

    @Bean
    @Primary
    fun shardingDataSource(config: DataSourceProperties, registry: MeterRegistry): DataSource {
        return createShardingDataSource(
            dataSourcePrefixName = DATA_SOURCE_NAME_PREFIX,
            databaseAlgorithmClassName = databaseAlgorithmClassName,
            tableAlgorithmClassName = tableAlgorithmClassName,
            dataSourceConfigs = config.dataSourceConfigs,
            tableRuleConfigs = config.tableRuleConfigs,
            bindingTableGroupConfigs = config.bindingTableGroupConfigs,
            registry = registry
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "sharding", name = ["migrationFlag"], havingValue = "Y")
    fun migratingShardingDataSource(config: DataSourceProperties, registry: MeterRegistry): DataSource {
        val migratingDataSourceConfigs = config.migratingDataSourceConfigs
        val migratingTableRuleConfigs = config.migratingTableRuleConfigs
        if (migratingDataSourceConfigs == null) {
            logger.warn("migratingDataSourceConfigs can not be empty")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = "migratingDataSourceConfigs can not be empty"
            )
        }
        return createShardingDataSource(
            dataSourcePrefixName = MIGRATING_DATA_SOURCE_NAME_PREFIX,
            databaseAlgorithmClassName = migratingDatabaseAlgorithmClassName,
            tableAlgorithmClassName = migratingTableAlgorithmClassName,
            dataSourceConfigs = migratingDataSourceConfigs,
            tableRuleConfigs = migratingTableRuleConfigs ?: config.tableRuleConfigs,
            bindingTableGroupConfigs = config.migratingBindingTableGroupConfigs ?: config.bindingTableGroupConfigs,
            registry = registry
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "sharding", name = ["archiveFlag"], havingValue = "Y")
    fun archiveShardingDataSource(config: DataSourceProperties, registry: MeterRegistry): DataSource {
        val archiveDataSourceConfigs = config.archiveDataSourceConfigs
        val archiveTableRuleConfigs = config.archiveTableRuleConfigs
        if (archiveDataSourceConfigs == null && archiveTableRuleConfigs == null) {
            logger.warn("archiveDataSourceConfigs and archiveTableRuleConfigs cannot be empty at the same time")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = "archiveDataSourceConfigs and archiveTableRuleConfigs cannot be empty at the same time"
            )
        }
        return createShardingDataSource(
            dataSourcePrefixName = ARCHIVE_DATA_SOURCE_NAME_PREFIX,
            databaseAlgorithmClassName = archiveDatabaseAlgorithmClassName,
            tableAlgorithmClassName = archiveTableAlgorithmClassName,
            dataSourceConfigs = archiveDataSourceConfigs ?: config.dataSourceConfigs,
            tableRuleConfigs = generateTableRuleConfigs(archiveTableRuleConfigs, config),
            bindingTableGroupConfigs = config.archiveBindingTableGroupConfigs ?: config.bindingTableGroupConfigs,
            registry = registry
        )
    }

    private fun generateTableRuleConfigs(
        tableRuleConfigs: List<TableRuleConfig>?,
        config: DataSourceProperties
    ): List<TableRuleConfig>? {
        return if (tableRuleConfigs.isNullOrEmpty() && defaultTableShardingNum > 1) {
            // 如果分表规则为空，则复用默认的分表规则
            val finalTableRuleConfigs = mutableListOf<TableRuleConfig>()
            config.tableRuleConfigs.forEach { tableRuleConfig ->
                val finalTableRuleConfig = tableRuleConfig.deepCopy<TableRuleConfig>()
                if (finalTableRuleConfig.broadcastFlag != true) {
                    finalTableRuleConfig.tableShardingStrategy = TableShardingStrategyEnum.SHARDING
                    finalTableRuleConfig.shardingNum = defaultTableShardingNum
                }
                finalTableRuleConfigs.add(finalTableRuleConfig)
            }
            finalTableRuleConfigs
        } else {
            tableRuleConfigs
        }
    }

    fun createShardingDataSource(
        dataSourcePrefixName: String,
        databaseAlgorithmClassName: String? = null,
        tableAlgorithmClassName: String? = null,
        dataSourceConfigs: List<DataSourceConfig>,
        tableRuleConfigs: List<TableRuleConfig>? = null,
        bindingTableGroupConfigs: List<BindingTableGroupConfig>? = null,
        registry: MeterRegistry
    ): DataSource {
        val singleRuleConfiguration = SingleRuleConfiguration() // 单表不再自动加载
        val shardingRuleConfig = ShardingRuleConfiguration()
        val broadcastRuleConfig = BroadcastRuleConfiguration(mutableListOf())
        // 生成单表规则
        singleRuleConfiguration.tables = listOf("*.*")
        // 设置分片表的路由规则
        val maxDsIndex = dataSourceConfigs.maxOfOrNull { it.index } ?: 0
        val bkTableRuleConfigs = shardingRuleConfig.tables
        val shardingTableRuleConfigs = tableRuleConfigs?.filter { it.broadcastFlag != true }
        if (!shardingTableRuleConfigs.isNullOrEmpty()) {
            shardingTableRuleConfigs.forEach { shardingTableRuleConfig ->
                bkTableRuleConfigs.add(
                    getTableRuleConfiguration(
                        dataSourcePrefixName = dataSourcePrefixName,
                        maxDsIndex = maxDsIndex,
                        tableRuleConfig = shardingTableRuleConfig
                    )
                )
            }
        }
        // 设置广播表的路由规则
        val broadcastTables = broadcastRuleConfig.tables
        val broadcastTableRuleConfigs = tableRuleConfigs?.filter { it.broadcastFlag == true }
        if (!broadcastTableRuleConfigs.isNullOrEmpty()) {
            broadcastTableRuleConfigs.forEach { broadcastTableRuleConfig ->
                broadcastTables.add(broadcastTableRuleConfig.name)
            }
        }
        // 	设置绑定表规则
        val bindingTableGroups = shardingRuleConfig.bindingTableGroups
        if (!bindingTableGroupConfigs.isNullOrEmpty()) {
            bindingTableGroupConfigs.forEach { bindingTableGroupConfig ->
                bindingTableGroups.add(
                    ShardingTableReferenceRuleConfiguration(
                        bindingTableGroupConfig.index.toString(),
                        bindingTableGroupConfig.rule
                    )
                )
            }
        }
        // 生成db分片算法配置
        if (!databaseAlgorithmClassName.isNullOrBlank()) {
            val dbShardingAlgorithmProps = Properties()
            dbShardingAlgorithmProps.setProperty(STRATEGY, STANDARD)
            dbShardingAlgorithmProps.setProperty(ALGORITHM_CLASS_NAME, databaseAlgorithmClassName)
            shardingRuleConfig.shardingAlgorithms[DB_SHARDING_ALGORITHM_NAME] =
                AlgorithmConfiguration(CLASS_BASED, dbShardingAlgorithmProps)
        }
        // 生成table分片算法配置
        if (!tableAlgorithmClassName.isNullOrBlank() && !tableRuleConfigs.isNullOrEmpty()) {
            val tableShardingAlgorithmProps = Properties()
            tableShardingAlgorithmProps.setProperty(STRATEGY, STANDARD)
            tableShardingAlgorithmProps.setProperty(ALGORITHM_CLASS_NAME, tableAlgorithmClassName)
            shardingRuleConfig.shardingAlgorithms[TABLE_SHARDING_ALGORITHM_NAME] =
                AlgorithmConfiguration(CLASS_BASED, tableShardingAlgorithmProps)
        }
        val dataSourceProperties = Properties()
        // 是否打印SQL解析和改写日志
        dataSourceProperties.setProperty("sql-show", shardingLogSwitch.toString())
        return ShardingSphereDataSourceFactory.createDataSource(
            dataSourceMap(dataSourcePrefixName, dataSourceConfigs, registry),
            listOf(singleRuleConfiguration, shardingRuleConfig, broadcastRuleConfig),
            dataSourceProperties
        )
    }

    /**
     * 获取分片表规则配置
     * @param dataSourcePrefixName 数据源数量大小
     * @param maxDsIndex 最大数据源索引值
     * @param tableRuleConfig 数据库表规则配置
     * @param logicTableSuffixName 数据库表规则配置
     * @return 分片表规则配置
     */
    fun getTableRuleConfiguration(
        dataSourcePrefixName: String,
        maxDsIndex: Int,
        tableRuleConfig: TableRuleConfig,
        logicTableSuffixName: String? = null
    ): ShardingTableRuleConfiguration? {
        // 生成实际节点规则
        val tableName = tableRuleConfig.name
        val databaseShardingStrategy = tableRuleConfig.databaseShardingStrategy
        val tableShardingStrategy = tableRuleConfig.tableShardingStrategy
        val maxTableIndex = tableRuleConfig.shardingNum - 1

        // 生成逻辑表名称
        val logicTableName = if (logicTableSuffixName.isNullOrBlank()) {
            tableName
        } else {
            "${tableName}_$logicTableSuffixName"
        }
        // 生成实际数据节点
        val actualDataNodes = when {
            // 分库分表场景
            databaseShardingStrategy != null && tableShardingStrategy == TableShardingStrategyEnum.SHARDING -> {
                if (databaseShardingStrategy == DatabaseShardingStrategyEnum.SPECIFY) {
                    "${dataSourcePrefixName}0.${logicTableName}_\${0..$maxTableIndex}"
                } else {
                    "$dataSourcePrefixName\${0..$maxDsIndex}.${logicTableName}_\${0..$maxTableIndex}"
                }
            }

            // 仅分库场景
            databaseShardingStrategy != null && tableShardingStrategy != TableShardingStrategyEnum.SHARDING -> {
                if (databaseShardingStrategy == DatabaseShardingStrategyEnum.SPECIFY) {
                    "${dataSourcePrefixName}0.$logicTableName"
                } else {
                    "$dataSourcePrefixName\${0..$maxDsIndex}.$logicTableName"
                }
            }

            // 仅分表场景
            databaseShardingStrategy == null && tableShardingStrategy == TableShardingStrategyEnum.SHARDING -> {
                "${dataSourcePrefixName}0.${logicTableName}_\${0..$maxTableIndex}"
            }

            // 默认场景（不分库不分表）
            else -> "${dataSourcePrefixName}0.$logicTableName"
        }

        val shardingTableRuleConfig = ShardingTableRuleConfiguration(tableName, actualDataNodes)
        logger.info(
            "BkShardingDataSourceConfiguration table:$tableName|databaseShardingStrategy: $databaseShardingStrategy|" +
                    "tableShardingStrategy:$tableShardingStrategy|actualDataNodes:$actualDataNodes "
        )

        // 设置表的分库策略
        shardingTableRuleConfig.databaseShardingStrategy = if (databaseShardingStrategy != null) {
            StandardShardingStrategyConfiguration(databaseShardingField, DB_SHARDING_ALGORITHM_NAME)
        } else {
            NoneShardingStrategyConfiguration()
        }

        // 设置表的分表策略
        shardingTableRuleConfig.tableShardingStrategy = if (tableShardingStrategy != null) {
            StandardShardingStrategyConfiguration(tableShardingField, TABLE_SHARDING_ALGORITHM_NAME)
        } else {
            NoneShardingStrategyConfiguration()
        }

        return shardingTableRuleConfig
    }
}
