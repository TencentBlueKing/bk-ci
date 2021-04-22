/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.mongo.dao.sharding

import com.mongodb.BasicDBList
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.MongoIndexResolver
import com.tencent.bkrepo.common.mongo.dao.util.ShardingUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.query.Query
import java.lang.reflect.Field
import javax.annotation.PostConstruct

/**
 * mongodb 支持分表的数据访问层抽象类
 */
abstract class ShardingMongoDao<E> : AbstractMongoDao<E>() {

    // 抽象类使用构造器注入不方便
    @Suppress("LateinitUsage")
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Value("\${sharding.count:#{null}}")
    private val fixedShardingCount: Int? = null

    /**
     * 分表Field
     */
    private val shardingField: Field
    /**
     * 分表列名
     */
    private val shardingColumn: String
    /**
     * 分表数
     */
    private var shardingCount: Int = 1

    init {
        @Suppress("LeakingThis")
        val fieldsWithShardingKey = getFieldsListWithAnnotation(classType, ShardingKey::class.java)
        require(fieldsWithShardingKey.size == 1) {
            "Only one field could be annotated with ShardingKey annotation but find ${fieldsWithShardingKey.size}!"
        }

        this.shardingField = fieldsWithShardingKey[0]
        this.shardingColumn = determineShardingColumn()
        this.shardingCount = determineShardingCount()
    }

    @PostConstruct
    private fun init() {
        updateShardingCountIfNecessary()
        ensureIndex()
    }

    private fun ensureIndex() {
        val start = System.currentTimeMillis()
        val indexDefinitions = MongoIndexResolver.resolveIndexFor(classType)
        val nonexistentIndexDefinitions = filterExistedIndex(indexDefinitions)
        nonexistentIndexDefinitions.forEach {
            for (i in 1..shardingCount) {
                val mongoTemplate = determineMongoTemplate()
                val collectionName = parseSequenceToCollectionName(i - 1)
                mongoTemplate.indexOps(collectionName).ensureIndex(it)
            }
        }

        val indexCount = shardingCount * indexDefinitions.size
        val consume = System.currentTimeMillis() - start

        logger.info("Ensure [$indexCount] index for sharding collection [$collectionName], consume [$consume] ms.")
    }

    private fun filterExistedIndex(indexDefinitions: List<IndexDefinition>): List<IndexDefinition> {
        val mongoTemplate = determineMongoTemplate()
        val collectionName = parseSequenceToCollectionName(0)
        val indexInfoList = mongoTemplate.indexOps(collectionName).indexInfo
        val indexNameList = indexInfoList.map { index -> index.name }
        return indexDefinitions.filter { index ->
            val indexOptions = index.indexOptions
            if (indexOptions.contains("name")) {
                val indexName = indexOptions.getString("name")
                !indexNameList.contains(indexName)
            } else true
        }
    }

    private fun shardingKeyToCollectionName(shardValue: Any): String {
        val shardingSequence = ShardingUtils.shardingSequenceFor(shardValue, shardingCount)
        return parseSequenceToCollectionName(shardingSequence)
    }

    fun parseSequenceToCollectionName(sequence: Int): String {
        return collectionName + "_" + sequence
    }

    private fun updateShardingCountIfNecessary() {
        if (fixedShardingCount != null) {
            this.shardingCount = fixedShardingCount
        }
    }

    private fun determineShardingCount(): Int {
        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        return ShardingUtils.shardingCountFor(shardingKey.count)
    }

    private fun determineShardingColumn(): String {
        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        if (shardingKey.column.isNotEmpty()) {
            return shardingKey.column
        }
        val fieldJavaClass = org.springframework.data.mongodb.core.mapping.Field::class.java
        val fieldAnnotation = AnnotationUtils.getAnnotation(shardingField, fieldJavaClass)
        if (fieldAnnotation != null && fieldAnnotation.value.isNotEmpty()) {
            return fieldAnnotation.value
        }
        return shardingField.name
    }

    override fun determineCollectionName(): String {
        if (classType.isAnnotationPresent(ShardingDocument::class.java)) {
            val document = classType.getAnnotation(ShardingDocument::class.java)
            return document.collection
        }
        return super.determineCollectionName()
    }

    override fun determineMongoTemplate(): MongoTemplate {
        return this.mongoTemplate
    }

    override fun determineCollectionName(entity: E): String {
        val shardingValue = FieldUtils.readField(shardingField, entity, true)
        requireNotNull(shardingValue) { "Sharding value can not be empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(query: Query): String {
        val shardingValue = determineCollectionName(query.queryObject)
        requireNotNull(shardingValue) { "Sharding value can not empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(aggregation: Aggregation): String {
        var shardingValue: Any? = null
        val pipeline = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)
        for (document in pipeline) {
            if (document.containsKey("\$match")) {
                val subDocument = document["\$match"]
                require(subDocument is Document)
                shardingValue = subDocument["projectId"]
                break
            }
        }

        requireNotNull(shardingValue) { "sharding value can not be empty!" }
        return shardingKeyToCollectionName(shardingValue)
    }

    private fun determineCollectionName(document: Document): Any? {
        for ((key, value) in document) {
            if (key == shardingColumn) return value
            if (key == "\$and") {
                require(value is BasicDBList)
                determineCollectionName(value)?.let { return it }
            }
        }
        return null
    }

    private fun determineCollectionName(list: BasicDBList): Any? {
        for (element in list) {
            require(element is Document)
            determineCollectionName(element)?.let { return it }
        }
        return null
    }

    override fun <T> findAll(clazz: Class<T>): List<T> {
        throw UnsupportedOperationException()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ShardingMongoDao::class.java)
    }
}
