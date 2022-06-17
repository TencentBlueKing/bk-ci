package com.tencent.bk.codecc.apiquery

import com.tencent.bk.codecc.apiquery.OpDBMongoConfig.Companion.MONGO_TEMPLATE
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["com.tencent.bk.codecc.apiquery.op.dao"], mongoTemplateRef = MONGO_TEMPLATE)
class OpDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "opMongoTemplate"
    }

    @Bean("opMongoProperties")
    @ConfigurationProperties(prefix = "spring.data.mongodb.opdb")
    fun mongoProperties() = MongoProperties()

    @Bean("opMongoDbFactory")
    fun mongoDbFactory(opMongoProperties: MongoProperties) = SimpleMongoClientDatabaseFactory(opMongoProperties.uri!!)

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate() = MongoTemplate(mongoDbFactory(mongoProperties()))
}