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
import com.tencent.devops.common.web.jasypt.DefaultEncryptor
import com.tencent.devops.misc.pojo.DataSourceConfig
import com.tencent.devops.misc.pojo.ProcessShardingDataSourceProperties
import com.zaxxer.hikari.HikariDataSource
import org.jasypt.encryption.StringEncryptor
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.env.Environment
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
@EnableConfigurationProperties(ProcessShardingDataSourceProperties::class)
class ProcessShardingDataSourceConfig : BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private val logger = LoggerFactory.getLogger(ProcessShardingDataSourceConfig::class.java)

    private lateinit var environment: Environment

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        // 使用 Environment 绑定配置属性
        val binder = Binder.get(environment)
        val properties =
            binder.bind("spring.datasource.process", ProcessShardingDataSourceProperties::class.java)
                .orElseThrow { IllegalStateException("Missing spring.datasource.process configuration") }
        val shardingMap = properties.sharding
        // 执行Bean注册逻辑
        val stringEncryptor = createDirectEncryptor()
        shardingMap.forEach { (shardId, config) ->
            registerShardBeans(
                registry = registry,
                shardId = shardId,
                config = config,
                stringEncryptor = stringEncryptor
            )
        }
    }

    private fun registerShardBeans(
        registry: BeanDefinitionRegistry,
        shardId: String,
        config: DataSourceConfig,
        stringEncryptor: StringEncryptor
    ) {
        if (config.url.isBlank()) {
            throw IllegalStateException("DataSource URL cannot be blank for shard $shardId")
        }
        // 1. 注册数据源 Bean
        val dataSourceBeanName = "${shardId}DataSource"
        val dataSourceDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(DataSource::class.java) {
                createDataSource("DBPool-Process-$shardId", config, stringEncryptor)
            }
            .setDestroyMethodName("close")
            .beanDefinition
        registry.registerBeanDefinition(dataSourceBeanName, dataSourceDefinition)

        // 2. 注册 DSLContext Bean
        val dslContextBeanName = "${shardId}DSLContext"
        val dslContextDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(DSLContext::class.java) {
                // 将registry转换为ConfigurableBeanFactory
                val beanFactory = registry as? ConfigurableBeanFactory ?: throw IllegalStateException(
                    "Registry must be an instance of ConfigurableBeanFactory to get DataSource bean"
                )
                // 通过转换后的beanFactory获取数据源
                val dataSource = beanFactory.getBean(dataSourceBeanName, DataSource::class.java)

                DSL.using(
                    DefaultConfiguration().apply {
                        set(dataSource)
                        set(SQLDialect.MYSQL)
                    }
                )
            }
            .beanDefinition
        registry.registerBeanDefinition(dslContextBeanName, dslContextDefinition)

        logger.info("Registered beans for shard $shardId: $dataSourceBeanName, $dslContextBeanName")
    }

    fun createDataSource(
        poolName: String,
        config: DataSourceConfig,
        stringEncryptor: StringEncryptor
    ): HikariDataSource {
        return HikariDataSource().apply {
            this.poolName = poolName
            jdbcUrl = config.url
            username = decryptWithJasypt(stringEncryptor, config.username)
            password = decryptWithJasypt(stringEncryptor, config.password)
            driverClassName = Driver::class.java.name
            minimumIdle = 1
            maximumPoolSize = 5
            idleTimeout = 60000
            connectionInitSql = config.initSql
            leakDetectionThreshold = config.leakDetectionThreshold
        }
    }

    private fun decryptWithJasypt(stringEncryptor: StringEncryptor, value: String): String {
        return if (value.startsWith("ENC(")) {
            stringEncryptor.decrypt(value.substring(4, value.length - 1))
        } else {
            value
        }
    }

    private fun createDirectEncryptor(): StringEncryptor {
        val key = environment.getProperty("enc.key", "")
        return DefaultEncryptor(key).apply {
            logger.info("Created direct StringEncryptor with key: ${key.take(3)}***")
        }
    }

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }
}
