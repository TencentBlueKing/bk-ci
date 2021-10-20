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

package com.tencent.devops.process.config

import com.mysql.cj.jdbc.Driver
import com.zaxxer.hikari.HikariDataSource
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration
import org.apache.shardingsphere.api.config.sharding.strategy.NoneShardingStrategyConfiguration
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.HashMap
import java.util.Properties
import javax.sql.DataSource

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(DataSourceAutoConfiguration::class, JooqAutoConfiguration::class)
@EnableTransactionManagement
class BkShardingDataSourceConfiguration {

    companion object {
        private const val PROJECT_ID_FIELD = "PROJECT_ID"
        private const val DEFAULT_DATA_SOURCE_NAME = "ds_0"
    }

    @Value("\${spring.datasource.url:#{null}}")
    private val datasourceUrl: String? = null
    @Value("\${spring.datasource.username:#{null}}")
    private val datasourceUsername: String? = null
    @Value("\${spring.datasource.password:#{null}}")
    private val datasourcePassword: String? = null
    @Value("\${spring.datasource.initSql:#{null}}")
    private val datasourceInitSql: String? = null
    @Value("\${spring.datasource.leakDetectionThreshold:#{0}}")
    private val datasouceLeakDetectionThreshold: Long = 0
    @Value("\${sharding.projectStrategy.tableConfig:#{null}}")
    private val shardingProjectStrategyTableConfig: String? = null
    @Value("\${sharding.broadcastStrategy.tableConfig:#{null}}")
    private val shardingBroadcastStrategyTableConfig: String? = null

    private fun dataSourceMap(): Map<String, DataSource> {
        val dataSourceMap: MutableMap<String, DataSource> = HashMap(2)
        // 开源版process分库方案默认只配置单实例
        dataSourceMap[DEFAULT_DATA_SOURCE_NAME] = HikariDataSource().apply {
            poolName = DEFAULT_DATA_SOURCE_NAME
            jdbcUrl = datasourceUrl
            username = datasourceUsername
            password = datasourcePassword
            driverClassName = Driver::class.java.name
            minimumIdle = 10
            maximumPoolSize = 50
            idleTimeout = 60000
            connectionInitSql = datasourceInitSql
            leakDetectionThreshold = datasouceLeakDetectionThreshold
        }
        return dataSourceMap
    }

    @Bean
    @Primary
    fun shardingDataSource(): DataSource {
        val shardingRuleConfig = ShardingRuleConfiguration()
        // 设置表的路由规则
        val tableRuleConfigs = shardingRuleConfig.tableRuleConfigs
        val projectStrategyTableNames = shardingProjectStrategyTableConfig?.split(",")
        if (!projectStrategyTableNames.isNullOrEmpty()) {
            projectStrategyTableNames.forEach { projectStrategyTableName ->
                tableRuleConfigs.add(getDefaultTableRuleConfiguration(projectStrategyTableName))
            }
        }
        // 设置广播表
        val broadcastTables = shardingRuleConfig.broadcastTables
        val broadcastStrategyTableNames = shardingBroadcastStrategyTableConfig?.split(",")
        if (!broadcastStrategyTableNames.isNullOrEmpty()) {
            broadcastStrategyTableNames.forEach { broadcastStrategyTableName ->
                broadcastTables.add(broadcastStrategyTableName)
            }
        }
        shardingRuleConfig.defaultTableShardingStrategyConfig = NoneShardingStrategyConfiguration()
        shardingRuleConfig.defaultDatabaseShardingStrategyConfig =
            StandardShardingStrategyConfiguration(PROJECT_ID_FIELD, BkProcessDatabaseShardingAlgorithm())
        val properties = Properties()
        // 是否打印SQL解析和改写日志
        properties.setProperty("sql.show", "false")
        return ShardingDataSourceFactory.createDataSource(dataSourceMap(), shardingRuleConfig, properties)
    }

    fun getDefaultTableRuleConfiguration(tableName: String): TableRuleConfiguration? {
        val tableRuleConfig = TableRuleConfiguration(tableName, "$DEFAULT_DATA_SOURCE_NAME.$tableName")
        tableRuleConfig.tableShardingStrategyConfig = NoneShardingStrategyConfiguration()
        tableRuleConfig.databaseShardingStrategyConfig =
            StandardShardingStrategyConfiguration(PROJECT_ID_FIELD, BkProcessDatabaseShardingAlgorithm())
        return tableRuleConfig
    }
}
