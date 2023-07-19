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

package com.tencent.devops.common.db.config

import com.mysql.cj.jdbc.Driver
import com.tencent.devops.common.db.pojo.DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.db.pojo.DataSourceProperties
import com.tencent.devops.common.db.pojo.DatabaseShardingStrategyEnum
import com.tencent.devops.common.db.pojo.TableRuleConfig
import com.tencent.devops.common.db.pojo.TableShardingStrategyEnum
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.Properties
import javax.sql.DataSource

@Suppress("LongParameterList", "MagicNumber")
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

    @Value("\${sharding.databaseShardingStrategy.shardingField:#{null}}")
    private val databaseShardingField: String? = null

    @Value("\${sharding.tableShardingStrategy.algorithmClassName:#{null}}")
    private val tableAlgorithmClassName: String? = null

    @Value("\${sharding.tableShardingStrategy.shardingField:#{null}}")
    private val tableShardingField: String? = null

    @Value("\${spring.datasource.minimumIdle:#{1}}")
    private val datasourceMinimumIdle: Int = 1

    @Value("\${spring.datasource.maximumPoolSize:#{50}}")
    private val datasourceMaximumPoolSize: Int = 50

    @Value("\${spring.datasource.idleTimeout:#{60000}}")
    private val datasourceIdleTimeout: Long = 60000

    private fun dataSourceMap(config: DataSourceProperties, registry: MeterRegistry): Map<String, DataSource> {
        val dataSourceMap: MutableMap<String, DataSource> = mutableMapOf()
        val dataSourceConfigs = config.dataSourceConfigs
        // 根据配置文件中的数据源配置项列表动态生成数据源集合
        dataSourceConfigs.forEach { dataSourceConfig ->
            val dataSourceName = "$DATA_SOURCE_NAME_PREFIX${dataSourceConfig.index}"
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
        }
    }

    @Bean
    fun shardingDataSource(config: DataSourceProperties, registry: MeterRegistry): DataSource {
        val shardingRuleConfig = ShardingRuleConfiguration()
        // 设置分片表的路由规则
        val dataSourceSize = config.dataSourceConfigs.size
        val tableRuleConfigs = shardingRuleConfig.tables
        val shardingTableRuleConfigs = config.tableRuleConfigs.filter { it.broadcastFlag != true }
        if (shardingTableRuleConfigs.isNotEmpty()) {
            shardingTableRuleConfigs.forEach { shardingTableRuleConfig ->
                tableRuleConfigs.add(getTableRuleConfiguration(dataSourceSize, shardingTableRuleConfig))
            }
        }
        // 设置广播表的路由规则
        val broadcastTables = shardingRuleConfig.broadcastTables
        val broadcastTableRuleConfigs = config.tableRuleConfigs.filter { it.broadcastFlag == true }
        if (broadcastTableRuleConfigs.isNotEmpty()) {
            broadcastTableRuleConfigs.forEach { broadcastTableRuleConfig ->
                broadcastTables.add(broadcastTableRuleConfig.name)
            }
        }
        // 	设置绑定表规则
        val bindingTableGroups = shardingRuleConfig.bindingTableGroups
        val bindingTableGroupConfigs = config.bindingTableGroupConfigs
        if (!bindingTableGroupConfigs.isNullOrEmpty()) {
            bindingTableGroupConfigs.forEach { bindingTableGroupConfig ->
                bindingTableGroups.add(bindingTableGroupConfig.rule)
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
        if (!tableAlgorithmClassName.isNullOrBlank()) {
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
            dataSourceMap(config, registry),
            listOf(shardingRuleConfig),
            dataSourceProperties
        )
    }

    /**
     * 获取分片表规则配置
     * @param dataSourceSize 数据源数量大小
     * @param tableRuleConfig 数据库表规则配置
     * @return 分片表规则配置
     */
    fun getTableRuleConfiguration(
        dataSourceSize: Int,
        tableRuleConfig: TableRuleConfig
    ): ShardingTableRuleConfiguration? {
        // 生成实际节点规则
        val tableName = tableRuleConfig.name
        val databaseShardingStrategy = tableRuleConfig.databaseShardingStrategy
        val tableShardingStrategy = tableRuleConfig.tableShardingStrategy
        val lastDsIndex = dataSourceSize - 1
        val lastTableIndex = tableRuleConfig.shardingNum - 1
        val actualDataNodes = if (databaseShardingStrategy != null &&
            tableShardingStrategy == TableShardingStrategyEnum.SHARDING
        ) {
            // 生成分库分表场景下的节点规则
            if (databaseShardingStrategy == DatabaseShardingStrategyEnum.SPECIFY) {
                "${DATA_SOURCE_NAME_PREFIX}0.${tableName}_\${0..$lastTableIndex}"
            } else {
                "$DATA_SOURCE_NAME_PREFIX\${0..$lastDsIndex}.${tableName}_\${0..$lastTableIndex}"
            }
        } else if (databaseShardingStrategy != null && tableShardingStrategy != TableShardingStrategyEnum.SHARDING) {
            // 生成分库场景下的节点规则
            if (databaseShardingStrategy == DatabaseShardingStrategyEnum.SPECIFY) {
                "${DATA_SOURCE_NAME_PREFIX}0.$tableName"
            } else {
                "$DATA_SOURCE_NAME_PREFIX\${0..$lastDsIndex}.$tableName"
            }
        } else if (databaseShardingStrategy == null && tableShardingStrategy == TableShardingStrategyEnum.SHARDING) {
            // 生成分表场景下的节点规则
            "${DATA_SOURCE_NAME_PREFIX}0.${tableName}_\${0..$lastTableIndex}"
        } else {
            "${DATA_SOURCE_NAME_PREFIX}0.$tableName"
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
