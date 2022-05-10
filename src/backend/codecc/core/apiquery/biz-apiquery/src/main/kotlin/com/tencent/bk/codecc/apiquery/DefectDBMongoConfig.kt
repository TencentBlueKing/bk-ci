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
    basePackages = ["com.tencent.bk.codecc.apiquery.defect.dao"],
    mongoTemplateRef = DefectDBMongoConfig.MONGO_TEMPLATE
)
class DefectDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "defectMongoTemplate"
    }

    @ConfigurationProperties(prefix = "spring.data.mongodb.defectdb")
    @Bean
    fun defectMongoProperties() = CodeCCMongoProperties()

    @Bean
    fun defectMongoDbFactory(defectMongoProperties: CodeCCMongoProperties) =
        SimpleMongoClientDatabaseFactory(defectMongoProperties.uri!!)

    @Bean
    fun mongoMappingContext() = MongoMappingContext()

    @Bean
    fun defectMappingMongoConverter(
        defectMongoDbFactory: SimpleMongoClientDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(defectMongoDbFactory)
        return MappingMongoConverter(dbRefResolver, mongoMappingContext)
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(defectMongoDbFactory: SimpleMongoClientDatabaseFactory,
                      defectMappingMongoConverter: MappingMongoConverter) =
        MongoTemplate(defectMongoDbFactory, defectMappingMongoConverter)
}