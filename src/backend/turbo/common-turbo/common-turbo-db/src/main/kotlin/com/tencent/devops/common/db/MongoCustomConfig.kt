package com.tencent.devops.common.db

import com.mongodb.ReadConcern
import com.mongodb.TransactionOptions
import com.mongodb.WriteConcern
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Suppress("MaxLineLength")
@Configuration
@EnableMongoRepositories(basePackages = ["com.tencent.devops.turbo"])
class MongoCustomConfig {

    @Bean
    fun transactionManager(factory: MongoDatabaseFactory): MongoTransactionManager {
        val transactionOptions = TransactionOptions.builder().readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.W1).build()
        return MongoTransactionManager(factory, transactionOptions)
    }

    @Bean
    fun customConverters(
        mongodatabaseFactory: MongoDatabaseFactory,
        mongoMappingContext: MongoMappingContext
    ): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(mongodatabaseFactory)
        val mappingMongoConverter = MappingMongoConverter(dbRefResolver, mongoMappingContext)
        mappingMongoConverter.setMapKeyDotReplacement("~")
        return mappingMongoConverter
    }
}
