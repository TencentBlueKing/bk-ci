package com.tencent.bk.codecc.apiquery

import com.fasterxml.jackson.core.JsonProcessingException
import com.mongodb.DBObject
import com.mongodb.MongoClientURI
import com.mongodb.util.JSON
import com.tencent.bk.codecc.apiquery.pojo.CodeCCMongoProperties
import com.tencent.devops.common.util.JsonUtil
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import java.io.IOException

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
        SimpleMongoDbFactory(MongoClientURI(taskMongoProperties.uri))

    @Bean
    fun taskMappingMongoConverter(
        taskMongoDbFactory: SimpleMongoDbFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(taskMongoDbFactory)
        return object : MappingMongoConverter(dbRefResolver, mongoMappingContext) {
            override fun <S> read(clazz: Class<S>, dbo: DBObject?): S? {
                if(null == dbo){
                    return null
                }
                val string = JSON.serialize(dbo)
                try {
                    return JsonUtil.getObjectMapper().readValue(string, clazz)
                } catch (e: IOException) {
                    throw RuntimeException(string, e)
                }
            }

            override fun write(obj: Any, dbo: DBObject) {
                var string: String? = null
                try {
                    string = JsonUtil.getObjectMapper().writeValueAsString(obj)
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(string, e)
                }

                dbo.putAll(JSON.parse(string) as DBObject)
            }
        }
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(taskMongoDbFactory: SimpleMongoDbFactory, taskMappingMongoConverter: MappingMongoConverter) =
        MongoTemplate(taskMongoDbFactory, taskMappingMongoConverter)
}