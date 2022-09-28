package com.tencent.bk.codecc.apiquery

import com.fasterxml.jackson.core.JsonProcessingException
import com.mongodb.DBObjectCodecProvider
import com.mongodb.DBRefCodecProvider
import com.mongodb.client.gridfs.codecs.GridFSFileCodecProvider
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider
import com.tencent.bk.codecc.apiquery.pojo.CodeCCMongoProperties
import org.bson.conversions.Bson
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import java.io.IOException
import com.tencent.devops.common.api.codecc.util.JsonUtil
import org.bson.Document
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry

@Configuration
@EnableMongoRepositories(
    basePackages = ["com.tencent.bk.codecc.apiquery.defect.dao"],
    mongoTemplateRef = DefectDBMongoConfig.MONGO_TEMPLATE
)
class DefectDBMongoConfig {

    companion object {
        const val MONGO_TEMPLATE = "defectMongoTemplate"
        private val DEFAULT_REGISTRY: CodecRegistry = CodecRegistries.fromProviders(
            listOf(
                ValueCodecProvider(),
                BsonValueCodecProvider(),
                DocumentCodecProvider(),
                DBRefCodecProvider(),
                DBObjectCodecProvider(),
                BsonValueCodecProvider(),
                GeoJsonCodecProvider(),
                GridFSFileCodecProvider()
            )
        )
        private val DEFAULT_BSON_TYPE_CLASS_MAP = BsonTypeClassMap()
        val documentCodec = DocumentCodec(DEFAULT_REGISTRY,DEFAULT_BSON_TYPE_CLASS_MAP)
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
        return object : MappingMongoConverter(dbRefResolver, mongoMappingContext) {
            override fun <S : Any?> read(clazz: Class<S>, bson: Bson): S {
                val string = (bson as Document).toJson(documentCodec)
                try {
                    return JsonUtil.getObjectMapper().readValue(string, clazz)
                } catch (e: IOException) {
                    throw RuntimeException(string, e)
                }
            }

            override fun write(obj: Any, bson: Bson) {
                var string: String? = null
                try {
                    string = JsonUtil.getObjectMapper().writeValueAsString(obj)
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(string, e)
                }
                (bson as Document).putAll(Document.parse(string, documentCodec))
            }
        }
    }

    @Bean(MONGO_TEMPLATE)
    fun mongoTemplate(defectMongoDbFactory: SimpleMongoClientDatabaseFactory, defectMappingMongoConverter: MappingMongoConverter) =
        MongoTemplate(defectMongoDbFactory, defectMappingMongoConverter)
}