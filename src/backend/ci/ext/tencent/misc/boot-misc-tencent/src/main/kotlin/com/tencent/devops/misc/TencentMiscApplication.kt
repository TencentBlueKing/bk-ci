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

package com.tencent.devops.misc

import com.mysql.cj.jdbc.Driver
import com.tencent.devops.common.service.MicroService
import com.tencent.devops.common.service.MicroServiceApplication
import com.zaxxer.hikari.HikariDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@MicroService
class TencentMiscApplication {

    @ConditionalOnMissingBean(name = ["dataSource"])
    class PrimaryDSConfiguration {

        @Bean
        @Primary
        fun dataSource(
            @Value("\${spring.datasource.process.url}")
            datasourceUrl: String,
            @Value("\${spring.datasource.process.username}")
            datasourceUsername: String,
            @Value("\${spring.datasource.process.password}")
            datasourcePassword: String,
            @Value("\${spring.datasource.process.initSql:#{null}}")
            datasourceInitSql: String? = null,
            @Value("\${spring.datasource.process.leakDetectionThreshold:#{0}}")
            datasouceLeakDetectionThreshold: Long = 0
        ): DataSource {
            return HikariDataSource().apply {
                poolName = "DBPool-Misc-Process"
                jdbcUrl = datasourceUrl
                username = datasourceUsername
                password = datasourcePassword
                driverClassName = Driver::class.java.name
                minimumIdle = 1
                maximumPoolSize = 10
                idleTimeout = 60000
                connectionInitSql = datasourceInitSql
                leakDetectionThreshold = datasouceLeakDetectionThreshold
            }
        }

        @Bean
        fun defaultJooqConfiguration(@Qualifier("dataSource") dataSource: DataSource): DefaultConfiguration {
            val configuration = DefaultConfiguration()
            configuration.set(SQLDialect.MYSQL)
            configuration.set(dataSource)
            return configuration
        }
    }
}

fun main(args: Array<String>) {
    MicroServiceApplication.run(TencentMiscApplication::class, args)
}
