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
import com.tencent.devops.misc.pojo.DataSourceConfig
import com.tencent.devops.misc.pojo.ProcessShardingDataSourceProperties
import com.zaxxer.hikari.HikariDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
class ProcessShardingDataSourceConfig(
    private val properties: ProcessShardingDataSourceProperties
) : BeanDefinitionRegistryPostProcessor {

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        properties.sharding.forEach { (shardId, config) ->
            registerShardBeans(registry, shardId, config)
        }
    }

    private fun registerShardBeans(
        registry: BeanDefinitionRegistry,
        shardId: String,
        config: DataSourceConfig
    ) {
        // 1. 注册DataSource（修复空URL问题）
        val dataSourceBeanName = "${shardId}DataSource"
        if (config.url.isBlank()) {
            throw IllegalStateException("DataSource URL cannot be blank for shard $shardId")
        }
        val dataSourceDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(DataSource::class.java) {
                createDataSource("DBPool-Process-$shardId", config)
            }
            .setDestroyMethodName("close")
            .beanDefinition
        registry.registerBeanDefinition(dataSourceBeanName, dataSourceDefinition)

        // 2. 注册 FactoryBean（用于创建 JooqConfig）
        val factoryBeanName = "${shardId}JooqConfigFactory"
        registry.registerBeanDefinition(factoryBeanName,
            BeanDefinitionBuilder.genericBeanDefinition(JooqConfigFactoryBean::class.java) {
                JooqConfigFactoryBean(dataSourceBeanName)
            }.beanDefinition
        )

        // 3. 注册 JooqConfig（通过 FactoryBean 创建）
        val jooqConfigBeanName = "${shardId}JooqConfiguration"
        val jooqConfigDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(org.jooq.Configuration::class.java)
            .apply {
                setFactoryMethodOnBean("getObject", factoryBeanName)
            }
            .addDependsOn(dataSourceBeanName)  // 添加 DataSource 依赖
            .addDependsOn(factoryBeanName)      // 添加 FactoryBean 依赖
            .beanDefinition
        registry.registerBeanDefinition(jooqConfigBeanName, jooqConfigDefinition)
    }

    fun createDataSource(
        poolName: String,
        config: DataSourceConfig
    ): HikariDataSource? {
        if (config.url.isBlank()) return null
        return HikariDataSource().apply {
            this.poolName = poolName
            jdbcUrl = config.url
            username = config.username
            password = config.password
            driverClassName = Driver::class.java.name
            minimumIdle = 1
            maximumPoolSize = 5
            idleTimeout = 60000
            connectionInitSql = config.initSql
            leakDetectionThreshold = config.leakDetectionThreshold
        }
    }

    // 定义FactoryBean实现延迟依赖解析
    class JooqConfigFactoryBean(
        private val dataSourceBeanName: String
    ) : FactoryBean<org.jooq.Configuration> {

        @Autowired
        private lateinit var beanFactory: BeanFactory

        override fun getObjectType() = org.jooq.Configuration::class.java

        override fun getObject(): org.jooq.Configuration {
            // 运行时动态获取DataSource（避免Bean初始化时序问题）
            val dataSource = beanFactory.getBean(dataSourceBeanName, DataSource::class.java)
            return DefaultConfiguration().apply {
                set(dataSource)
                set(SQLDialect.MYSQL)
            }
        }
    }
}
