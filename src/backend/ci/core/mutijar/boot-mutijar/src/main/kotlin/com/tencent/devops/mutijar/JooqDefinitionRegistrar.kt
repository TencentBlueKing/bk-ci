package com.tencent.devops.mutijar

import com.tencent.devops.common.db.listener.BkJooqExecuteListener
import org.jooq.SQLDialect
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata

@Configuration
class JooqDefinitionRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
        importBeanNameGenerator: BeanNameGenerator
    ) {
        multiDataSource.forEach { dataSource ->
            val finalDataSource = if (dataSource == "process") "shardingDataSource" else
                "${dataSource}DataSource"
            val connectionProvider = BeanDefinitionBuilder.genericBeanDefinition(
                DataSourceConnectionProvider::class.java
            ).addConstructorArgReference(finalDataSource)
            registry.registerBeanDefinition("${dataSource}ConnectionProvider", connectionProvider.beanDefinition)
            val beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultConfiguration::class.java) {
                val configuration = DefaultConfiguration()
                configuration.set(SQLDialect.MYSQL)
                configuration.settings().isRenderSchema = false
                configuration.set(DefaultExecuteListenerProvider(BkJooqExecuteListener()))
                configuration
            }
            beanDefinitionBuilder.addPropertyReference("connectionProvider", "${dataSource}ConnectionProvider")
            registry.registerBeanDefinition("${dataSource}JooqConfiguration", beanDefinitionBuilder.beanDefinition)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceDefinitionRegistrar::class.java)
        private val multiDataSource = System.getProperty("devops.multi.from").split(",")
    }
}
