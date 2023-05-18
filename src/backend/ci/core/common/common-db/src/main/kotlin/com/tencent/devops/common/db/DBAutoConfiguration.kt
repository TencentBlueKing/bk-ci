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

package com.tencent.devops.common.db

import com.mysql.cj.jdbc.Driver
import com.tencent.devops.common.db.config.DBBaseConfiguration
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 *
 * Powered By Tencent
 */
@Configuration
@PropertySource("classpath:/common-db.properties")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(DataSourceAutoConfiguration::class, JooqAutoConfiguration::class)
@Import(DBBaseConfiguration::class)
@EnableTransactionManagement
class DBAutoConfiguration {

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
    @Value("\${spring.datasource.minimumIdle:#{1}}")
    private val datasourceMinimumIdle: Int = 1
    @Value("\${spring.datasource.maximumPoolSize:#{50}}")
    private val datasourceMaximumPoolSize: Int = 50
    @Value("\${spring.datasource.idleTimeout:#{60000}}")
    private val datasourceIdleTimeout: Long = 60000

    @Bean
    @Primary
    fun dataSource(): DataSource {
        if (datasourceUrl.isNullOrBlank()) {
            throw IllegalArgumentException("Database connection address is not configured")
        }
        return HikariDataSource().apply {
            poolName = "DBPool-Main"
            jdbcUrl = datasourceUrl
            username = datasourceUsername
            password = datasourcePassword
            driverClassName = Driver::class.java.name
            minimumIdle = datasourceMinimumIdle
            maximumPoolSize = datasourceMaximumPoolSize
            idleTimeout = datasourceIdleTimeout
            connectionInitSql = datasourceInitSql
            leakDetectionThreshold = datasouceLeakDetectionThreshold
        }
    }
}
