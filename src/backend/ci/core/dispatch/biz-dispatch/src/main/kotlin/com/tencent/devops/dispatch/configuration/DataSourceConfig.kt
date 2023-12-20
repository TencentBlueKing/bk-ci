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

package com.tencent.devops.dispatch.configuration

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
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-Dispatch",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    @Bean
    fun dispatchKubernetesDataSource(
        @Value("\${spring.datasource.dispatchKubernetes.url}")
        datasourceUrl: String,
        @Value("\${spring.datasource.dispatchKubernetes.username}")
        datasourceUsername: String,
        @Value("\${spring.datasource.dispatchKubernetes.password}")
        datasourcePassword: String,
        @Value("\${spring.datasource.dispatchKubernetes.initSql:#{null}}")
        datasourceInitSql: String? = null,
        @Value("\${spring.datasource.dispatchKubernetes.leakDetectionThreshold:#{0}}")
        datasourceLeakDetectionThreshold: Long = 0
    ): DataSource {
        return hikariDataSource(
            datasourcePoolName = "DBPool-DispatchKubernetes",
            datasourceUrl = datasourceUrl,
            datasourceUsername = datasourceUsername,
            datasourcePassword = datasourcePassword,
            datasourceInitSql = datasourceInitSql,
            datasourceLeakDetectionThreshold = datasourceLeakDetectionThreshold
        )
    }

    private fun hikariDataSource(
        datasourcePoolName: String,
        datasourceUrl: String,
        datasourceUsername: String,
        datasourcePassword: String,
        datasourceInitSql: String?,
        datasourceLeakDetectionThreshold: Long
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
            leakDetectionThreshold = datasourceLeakDetectionThreshold
        }
    }
}
