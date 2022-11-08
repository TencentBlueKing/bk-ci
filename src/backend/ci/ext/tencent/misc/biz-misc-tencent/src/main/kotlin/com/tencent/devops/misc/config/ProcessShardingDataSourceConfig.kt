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

package com.tencent.devops.misc.config

import com.mysql.jdbc.Driver
import com.zaxxer.hikari.HikariDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
class ProcessShardingDataSourceConfig {

    @Bean
    fun p1DataSource(
        @Value("\${spring.datasource.process.sharding.p1.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p1.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p1.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p1.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p1.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P1",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p1JooqConfiguration(
        @Qualifier("p1DataSource")
        p1DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p1DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p1DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    @Bean
    fun p2DataSource(
        @Value("\${spring.datasource.process.sharding.p2.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p2.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p2.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p2.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p2.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P2",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p2JooqConfiguration(
        @Qualifier("p2DataSource")
        p2DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p2DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p2DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    @Bean
    fun p3DataSource(
        @Value("\${spring.datasource.process.sharding.p3.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p3.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p3.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p3.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p3.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P3",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p3JooqConfiguration(
        @Qualifier("p3DataSource")
        p3DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p3DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p3DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    @Bean
    fun p4DataSource(
        @Value("\${spring.datasource.process.sharding.p4.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p4.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p4.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p4.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p4.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P4",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p4JooqConfiguration(
        @Qualifier("p4DataSource")
        p4DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p4DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p4DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    @Bean
    fun p5DataSource(
        @Value("\${spring.datasource.process.sharding.p5.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p5.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p5.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p5.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p5.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P5",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p5JooqConfiguration(
        @Qualifier("p5DataSource")
        p5DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p5DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p5DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    @Bean
    fun p6DataSource(
        @Value("\${spring.datasource.process.sharding.p6.url:}")
        datasourceUrl: String = "",
        @Value("\${spring.datasource.process.sharding.p6.username:}")
        datasourceUsername: String = "",
        @Value("\${spring.datasource.process.sharding.p6.password:}")
        datasourcePassword: String = "",
        @Value("\${spring.datasource.process.sharding.p6.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.process.sharding.p6.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource? {
        return createDataSource(
            datasourcePoolName = "DBPool-Process-P6",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun p6JooqConfiguration(
        @Qualifier("p6DataSource")
        p6DataSource: DataSource?
    ): DefaultConfiguration? {
        return if (p6DataSource != null) {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(p6DataSource)
            configuration.settings().isRenderSchema = false
            configuration
        } else {
            null
        }
    }

    private fun createDataSource(
        datasourcePoolName: String,
        datasourceUrl: String,
        datasourceUsername: String,
        datasourcePassword: String,
        datasourceInitSql: String?,
        datasourceLeakDetectionThreshold: Long
    ): HikariDataSource? {
        if (datasourceUrl.isNotBlank()) {
            return HikariDataSource().apply {
                poolName = datasourcePoolName
                jdbcUrl = datasourceUrl
                username = datasourceUsername
                password = datasourcePassword
                driverClassName = Driver::class.java.name
                minimumIdle = 1
                maximumPoolSize = 5
                idleTimeout = 60000
                connectionInitSql = datasourceInitSql
                leakDetectionThreshold = datasourceLeakDetectionThreshold
            }
        } else {
            return null
        }
    }
}
