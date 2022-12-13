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

import com.mysql.cj.jdbc.Driver
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 *
 * Powered By Tencent
 */
@Suppress("LongParameterList")
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
class DataSourceConfig {

    @Bean
    @Primary
    fun projectDataSource(
        @Value("\${spring.datasource.project.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.project.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.project.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.project.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.project.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Project",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun repositoryDataSource(
        @Value("\${spring.datasource.repository.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.repository.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.repository.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.repository.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.repository.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Repository",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun dispatchDataSource(
        @Value("\${spring.datasource.dispatch.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatch.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatch.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatch.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatch.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Dispatch",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun pluginDataSource(
        @Value("\${spring.datasource.plugin.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.plugin.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.plugin.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.plugin.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.plugin.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Plugin",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun qualityDataSource(
        @Value("\${spring.datasource.quality.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.quality.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.quality.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.quality.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.quality.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Quality",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun artifactoryDataSource(
        @Value("\${spring.datasource.artifactory.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.artifactory.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.artifactory.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.artifactory.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.artifactory.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Artifactory",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun environmentDataSource(
        @Value("\${spring.datasource.environment.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.environment.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.environment.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.environment.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.environment.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Environment",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    private fun hikariDataSource(
        datasourcePoolName: String,
        datasourceUrl: String,
        datasourceUsername: String,
        datasourcePassword: String,
        datasourceInitSql: String?,
        datasouceLeakDetectionThreshold: Long
    ): HikariDataSource {
        return HikariDataSource().apply {
            poolName = datasourcePoolName
            jdbcUrl = datasourceUrl
            username = datasourceUsername
            password = datasourcePassword
            driverClassName = Driver::class.java.name
            minimumIdle = 1
            maximumPoolSize = 8
            idleTimeout = 60000
            connectionInitSql = datasourceInitSql
            leakDetectionThreshold = datasouceLeakDetectionThreshold
        }
    }
}
