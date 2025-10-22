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

import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.db.pojo.MIGRATING_SHARDING_DSL_CONTEXT
import org.jooq.DSLContext
import org.jooq.ExecuteListenerProvider
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
@Import(BkShardingDataSourceConfiguration::class, DBBaseConfiguration::class)
@AutoConfigureAfter(DBBaseConfiguration::class)
class BkShardingJooqConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "sharding", name = ["defaultFlag"], havingValue = "Y", matchIfMissing = true)
    fun shardingDslContext(
        @Qualifier("shardingDataSource")
        shardingDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DSLContext {
        return createDslContext(shardingDataSource, executeListenerProviders)
    }

    @Bean(name = [MIGRATING_SHARDING_DSL_CONTEXT])
    @ConditionalOnProperty(prefix = "sharding", name = ["migrationFlag"], havingValue = "Y")
    fun migratingShardingDslContext(
        @Qualifier("migratingShardingDataSource")
        migratingShardingDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DSLContext {
        return createDslContext(migratingShardingDataSource, executeListenerProviders)
    }

    @Bean(name = [ARCHIVE_SHARDING_DSL_CONTEXT])
    @ConditionalOnProperty(prefix = "sharding", name = ["archiveFlag"], havingValue = "Y")
    fun archiveShardingDslContext(
        @Qualifier("archiveShardingDataSource")
        archiveShardingDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DSLContext {
        return createDslContext(archiveShardingDataSource, executeListenerProviders)
    }

    private fun createDslContext(
        shardingDataSource: DataSource,
        executeListenerProviders: ObjectProvider<ExecuteListenerProvider>
    ): DSLContext {
        val configuration = DefaultConfiguration()
            .set(shardingDataSource)
            .set(
                Settings().withRenderSchema(false)
                    .withExecuteLogging(true)
                    .withRenderFormatted(false)
            )
            .set(SQLDialect.MYSQL)
        for (provider in executeListenerProviders) {
            logger.info("provider class: {}", provider.provide().javaClass)
        }
        configuration.set(*executeListenerProviders.stream().toArray { size ->
            arrayOfNulls<ExecuteListenerProvider>(size)
        })
        return DSL.using(configuration)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkShardingJooqConfiguration::class.java)
    }
}
