/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.configuration

import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.mongo.MongoClientFactory
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoDatabaseFactorySupport
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.MongoConverter
import java.util.stream.Collectors

@Configuration
class MultipleMongoConfig {

    // primary mongo

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.data.mongodb")
    fun mongoProperties(): MongoProperties {
        return MongoProperties()
    }

    @Bean
    @Primary
    fun mongo(
        properties: MongoProperties,
        environment: Environment,
        builderCustomizers: ObjectProvider<MongoClientSettingsBuilderCustomizer?>,
        settings: ObjectProvider<MongoClientSettings?>
    ): MongoClient {
        return MongoClientFactory(
            properties,
            environment,
            builderCustomizers.orderedStream().collect(Collectors.toList())
        ).createMongoClient(settings.ifAvailable)
    }

    @Bean
    @Primary
    fun mongoDatabaseFactory(mongoClient: MongoClient, properties: MongoProperties): MongoDatabaseFactorySupport<*> {
        return SimpleMongoClientDatabaseFactory(mongoClient, properties.mongoClientDatabase)
    }

    @Bean
    @Primary
    fun mongoTemplate(factory: MongoDatabaseFactory, converter: MongoConverter): MongoTemplate {
        return MongoTemplate(factory, converter)
    }

    // scanner mongo

    @Bean(BEAN_NAME_SCANNER_MONGO_PROPERTIES)
    @ConfigurationProperties(prefix = "scanner.spring.data.mongodb")
    fun scannerMongoProperties(): MongoProperties {
        return MongoProperties()
    }

    @Bean(BEAN_NAME_SCANNER_MONGO_CLIENT)
    fun scannerMongo(
        @Qualifier(BEAN_NAME_SCANNER_MONGO_PROPERTIES) properties: MongoProperties,
        environment: Environment,
        builderCustomizers: ObjectProvider<MongoClientSettingsBuilderCustomizer?>,
        settings: ObjectProvider<MongoClientSettings?>
    ): MongoClient {
        return MongoClientFactory(
            properties,
            environment,
            builderCustomizers.orderedStream().collect(Collectors.toList())
        ).createMongoClient(settings.ifAvailable)
    }

    @Bean(BEAN_NAME_SCANNER_MONGO_DATABASE_FACTORY)
    fun scannerMongoDatabaseFactory(
        @Qualifier(BEAN_NAME_SCANNER_MONGO_CLIENT) mongoClient: MongoClient,
        @Qualifier(BEAN_NAME_SCANNER_MONGO_PROPERTIES) properties: MongoProperties
    ): MongoDatabaseFactorySupport<*> {
        return SimpleMongoClientDatabaseFactory(mongoClient, properties.mongoClientDatabase)
    }

    @Bean(BEAN_NAME_SCANNER_MONGO_TEMPLATE)
    fun scannerMongoTemplate(
        @Qualifier(BEAN_NAME_SCANNER_MONGO_DATABASE_FACTORY) factory: MongoDatabaseFactory,
        converter: MongoConverter
    ): MongoTemplate {
        return MongoTemplate(factory, converter)
    }

    companion object {
        private const val BEAN_NAME_SCANNER_MONGO_PROPERTIES = "scannerMongoProperties"
        private const val BEAN_NAME_SCANNER_MONGO_CLIENT = "scannerMongoClient"
        private const val BEAN_NAME_SCANNER_MONGO_DATABASE_FACTORY = "scannerMongoDatabaseFactory"
        const val BEAN_NAME_SCANNER_MONGO_TEMPLATE = "scannerMongoTemplate"
    }
}
