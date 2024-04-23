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
        multiModuleName.forEach forEach@{ moduleName ->
            logger.info("register datasource bean definitions :$moduleName")
            registerBeanDefinition(
                moduleName = moduleName,
                registry = registry
            )
        }
    }

    @Suppress("LongParameterList")
    fun registerBeanDefinition(
        moduleName: String,
        registry: BeanDefinitionRegistry
    ) {
        val beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HikariDataSource::class.java)
            .addPropertyValue("poolName", "DBPool-$moduleName")
            .addPropertyValue("jdbcUrl", getModuleJdbcUrl(moduleName))
            .addPropertyValue("username", dataSourceUserName)
            .addPropertyValue("password", dataSourcePassword)
            .addPropertyValue("driverClassName", Driver::class.java.name)
            .addPropertyValue("minimumIdle", 10)
            .addPropertyValue("maximumPoolSize", 50)
            .addPropertyValue("idleTimeout", 60000)
            .setPrimary(false)
        registry.registerBeanDefinition(
            "${convertToCamelCase(moduleName)}DataSource",
            beanDefinitionBuilder.beanDefinition
        )
    }

    fun getModuleJdbcUrl(moduleName: String): String {
        return "jdbc:mysql://$dataSourceUrl/devops_ci_$moduleName?useSSL=false&autoReconnect=true&" +
            "serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8" +
            "&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES," +
            "NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION%27"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceDefinitionRegistrar::class.java)
        private val dataSourceUrl = System.getProperty("spring.datasource.url")
        private val dataSourceUserName = System.getProperty("spring.datasource.username")
        private val dataSourcePassword = System.getProperty("spring.datasource.password")
        private val notNeedDataSourceService = listOf(
            "buildless", "metrics", "monitoring", "worker", "process", "websocket", "dispatch-docker"
        )
        private val multiModuleName = System.getProperty("devops.multi.from")
            .split(",").filterNot { notNeedDataSourceService.contains(it) }
    }
}
