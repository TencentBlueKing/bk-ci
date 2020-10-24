/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

/**
 * mongodb 4.0+开始支持事物，但springboot data mongo为了兼容老版本不出错，默认不开启事物
 * 同时，mongodb 事物控制只能用与多副本集的情况，否则会报错，因此添加了enabled开关
 */
@Configuration
@PropertySource("classpath:common-mongo.properties")
class MongoAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = ["spring.data.mongodb.transaction.enabled"], matchIfMissing = true)
    fun transactionManager(mongoDatabaseFactory: MongoDatabaseFactory): MongoTransactionManager {
        return MongoTransactionManager(mongoDatabaseFactory)
    }

    /**
     * remove _class
     */
    @Bean
    fun mappingMongoConverter(mongoDatabaseFactory: MongoDatabaseFactory): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(mongoDatabaseFactory)

        val conversions = MongoCustomConversions(emptyList<Any>())
        val mappingContext = MongoMappingContext()
        mappingContext.setSimpleTypeHolder(conversions.simpleTypeHolder)
        mappingContext.afterPropertiesSet()

        val converter = MappingMongoConverter(dbRefResolver, mappingContext)
        converter.setTypeMapper(DefaultMongoTypeMapper(null))
        converter.afterPropertiesSet()

        return converter
    }
}
