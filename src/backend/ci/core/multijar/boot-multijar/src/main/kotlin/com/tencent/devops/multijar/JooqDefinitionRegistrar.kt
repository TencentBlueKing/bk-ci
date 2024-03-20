package com.tencent.devops.multijar

import com.tencent.devops.common.db.listener.BkJooqExecuteListener
import com.tencent.devops.common.util.RegexUtils.convertToCamelCase
import org.jooq.SQLDialect
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.Ordered
import org.springframework.core.type.AnnotationMetadata

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class JooqDefinitionRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
        importBeanNameGenerator: BeanNameGenerator
    ) {
        multiModuleName.forEach { moduleName ->
            // 将dispatch-kubernetes类似字符串，转化为dispatchKubernetes
            val finalModuleName = convertToCamelCase(moduleName)
            logger.info("register Jooq configuration bean definition $finalModuleName")
            val dataSource = if (finalModuleName == "process" || finalModuleName == "metrics") {
                // process以及metrics使用分库分表数据源
                "shardingDataSource"
            } else {
                "${finalModuleName}DataSource"
            }

            val dataSourceConnectionProvider = BeanDefinitionBuilder.genericBeanDefinition(DataSourceConnectionProvider::class.java)
                .addConstructorArgReference(dataSource)
            registry.registerBeanDefinition(
                "${finalModuleName}ConnectionProvider",
                dataSourceConnectionProvider.beanDefinition
            )
            val beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultConfiguration::class.java) {
                val configuration = DefaultConfiguration()
                configuration.set(SQLDialect.MYSQL)
                configuration.settings().isRenderSchema = false
                configuration.set(DefaultExecuteListenerProvider(BkJooqExecuteListener()))
                configuration
            }
            beanDefinitionBuilder.addPropertyReference("connectionProvider", "${finalModuleName}ConnectionProvider")
            registry.registerBeanDefinition(
                "${finalModuleName}JooqConfiguration",
                beanDefinitionBuilder.beanDefinition
            )
        }
    }

    companion object {
        private val notNeedJooqConfigurationModule = listOf("buildless", "misc", "monitoring", "worker", "websocket")
        private val multiModuleName = System.getProperty("devops.multi.from").split(",")
            .filterNot { notNeedJooqConfigurationModule.contains(it) }
        private val logger = LoggerFactory.getLogger(JooqDefinitionRegistrar::class.java)
    }
}
