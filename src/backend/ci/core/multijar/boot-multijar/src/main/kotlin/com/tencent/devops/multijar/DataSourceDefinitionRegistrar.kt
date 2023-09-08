package com.tencent.devops.multijar

import com.mysql.jdbc.Driver
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.Ordered
import org.springframework.core.io.ClassPathResource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.transaction.annotation.EnableTransactionManagement


@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableTransactionManagement
class DataSourceDefinitionRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
        importBeanNameGenerator: BeanNameGenerator
    ) {
        multiDataSource.forEach { dataSource ->
            logger.info("DataSourceDefinitionRegistrar:$dataSource")
            val resource = ClassPathResource("application-$dataSource.yml")
            val yamlFactory = YamlPropertiesFactoryBean()
            yamlFactory.setResources(resource)
            val properties = yamlFactory.getObject()!!
            val dataSourceUrl = properties.getProperty("spring.datasource.url")
            val datasourceUsername = properties.getProperty("spring.datasource.username")
            val datasourcePassword = properties.getProperty("spring.datasource.password")
            val datasourceInitSql = properties.getProperty("spring.datasource.initSql")
            val leakDetectionThreshold = properties.getProperty("spring.datasource.leakDetectionThreshold") ?: 0
            val beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HikariDataSource::class.java)
                .addPropertyValue("poolName", "DBPool-$dataSource")
                .addPropertyValue("jdbcUrl", dataSourceUrl)
                .addPropertyValue("username", datasourceUsername!!)
                .addPropertyValue("password", datasourcePassword!!)
                .addPropertyValue("driverClassName", Driver::class.java.name)
                .addPropertyValue("minimumIdle", 10)
                .addPropertyValue("maximumPoolSize", 50)
                .addPropertyValue("idleTimeout", 60000)
                .addPropertyValue("connectionInitSql", datasourceInitSql)
                .addPropertyValue("leakDetectionThreshold", leakDetectionThreshold)
                .setPrimary(false)
            registry.registerBeanDefinition("${dataSource}DataSource", beanDefinitionBuilder.beanDefinition)
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceDefinitionRegistrar::class.java)
        private val notNeedDataSourceService = listOf(
            "buildless", "dockerhost", "metrics", "misc", "monitoring", "process","worker","websocket"
        )

        private
        val multiDataSource = System.getProperty("devops.multi.from")
            .split(",").filterNot { notNeedDataSourceService.contains(it) }
    }
}
