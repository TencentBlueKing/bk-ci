package com.tencent.bk.codecc.apiquery

import com.tencent.bk.codecc.apiquery.pojo.CodeCCMongoProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(
    basePackages = ["com.tencent.bk.codecc.apiquery.task.dao"],
    mongoTemplateRef = TaskDBMongoConfig.MONGO_TEMPLATE
)
class TaskDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "taskMongoTemplate"
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.data.mongodb.taskdb")
    fun taskMongoProperties() = CodeCCMongoProperties()

    @Bean
    fun taskMongoDbFactory(taskMongoProperties: CodeCCMongoProperties) =
        SimpleMongoClientDatabaseFactory(taskMongoProperties.uri!!)

    @Bean
    fun taskMappingMongoConverter(
        taskMongoDbFactory: SimpleMongoClientDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(taskMongoDbFactory)
        return MappingMongoConverter(dbRefResolver, mongoMappingContext)
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(taskMongoDbFactory: SimpleMongoClientDatabaseFactory,
                      taskMappingMongoConverter: MappingMongoConverter) =
        MongoTemplate(taskMongoDbFactory, taskMappingMongoConverter)
}