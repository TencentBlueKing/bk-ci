package com.tencent.devops.multijar

import com.mysql.jdbc.Driver
import com.tencent.devops.common.util.RegexUtils.convertToCamelCase
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.Ordered
import org.springframework.core.type.AnnotationMetadata
import org.springframework.transaction.annotation.EnableTransactionManagement


@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
@Suppress("MaxLineLength")
class DataSourceDefinitionRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
        importBeanNameGenerator: BeanNameGenerator
    ) {
        multiDataSource.forEach forEach@{ dataSourceName ->
            logger.info("register beanDefinitions :$dataSourceName")
            registerBeanDefinition(
                dataSourceName = dataSourceName,
                registry = registry
            )
        }
    }

    @Suppress("LongParameterList")
    fun  registerBeanDefinition(
        dataSourceName: String,
        registry: BeanDefinitionRegistry
    ) {
        val beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HikariDataSource::class.java)
            .addPropertyValue("poolName", "DBPool-$dataSourceName")
            .addPropertyValue(
                "jdbcUrl", "jdbc:mysql://${System.getProperty("spring.datasource.url")}/devops_ci_$dataSourceName?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8" +
                "&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION%27"
            )
            .addPropertyValue("username", System.getProperty("spring.datasource.username"))
            .addPropertyValue("password", System.getProperty("spring.datasource.password"))
            .addPropertyValue("driverClassName", Driver::class.java.name)
            .addPropertyValue("minimumIdle", 10)
            .addPropertyValue("maximumPoolSize", 50)
            .addPropertyValue("idleTimeout", 60000)
            .setPrimary(false)
        registry.registerBeanDefinition("${convertToCamelCase(dataSourceName)}DataSource", beanDefinitionBuilder.beanDefinition)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceDefinitionRegistrar::class.java)
        private val notNeedDataSourceService = listOf(
            "buildless", "dockerhost", "metrics", "monitoring", "worker", "process",
            "websocket", "dispatch-docker"
        )
        private val multiDataSource = System.getProperty("devops.multi.from")
            .split(",").filterNot { notNeedDataSourceService.contains(it) }
    }
}
