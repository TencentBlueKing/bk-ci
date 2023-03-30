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

package com.tencent.devops.dispatch.devcloud.config

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
    fun dispatchDevCloudDataSource(
        @Value("\${spring.datasource.dispatchDevCloud.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatchDevCloud.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatchDevCloud.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatchDevCloud.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatchDevCloud.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-DispatchDevCloud",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun dispatchMacosDataSource(
        @Value("\${spring.datasource.dispatchMacos.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatchMacos.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatchMacos.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatchMacos.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatchMacos.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-DispatchMacos",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun dispatchWindowsDataSource(
        @Value("\${spring.datasource.dispatchWindows.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatchWindows.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatchWindows.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatchWindows.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatchWindows.leakDetectionThreshold:#{0}}")
        datasouceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-DispatchWindows",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasouceLeakDetectionThreshold
        )
    }

    @Bean
    fun dispatchCodeccDataSource(
        @Value("\${spring.datasource.dispatchCodecc.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatchCodecc.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatchCodecc.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatchCodecc.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatchCodecc.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-dispatchCodecc",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasouceLeakDetectionThreshold = datasourceLeakDetectionThreshold
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
