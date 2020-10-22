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

package com.tencent.bkrepo.common.mongo.dao.sharding

import com.mongodb.BasicDBList
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.MongoIndexResolver
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

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
    private val shardingCount: Int

    init {
        @Suppress("LeakingThis")
        val fieldsWithShardingKey = getFieldsListWithAnnotation(classType, ShardingKey::class.java)
        require(fieldsWithShardingKey.size == 1) { "only one field could be annotated with ShardingKey annotation but find ${fieldsWithShardingKey.size}!" }

        this.shardingField = fieldsWithShardingKey[0]
        this.shardingColumn = determineShardingColumn()

        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        require(shardingKey.count > 0) { "Illegal sharding count: [${shardingKey.count}]" }
        this.shardingCount = shardingCountFor(shardingKey.count)
    }

    @PostConstruct
    private fun init() {
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

        logger.info("ensure [$indexCount] index for sharding collection [$collectionName], consume [$consume] ms totally")
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

    private fun shardingCountFor(i: Int): Int {
        require(i >= 0) { "Illegal initial shard count : $i" }
        var result = if (i > MAXIMUM_CAPACITY) MAXIMUM_CAPACITY else i
        result = tableSizeFor(result)
        if (i != result) {
            logger.warn("Bad initial shard count : [$i], converted to : [$result]")
        }
        return result
    }

    private fun tableSizeFor(cap: Int): Int {
        // 减一的目的在于如果cap本身就是2的次幂，保证结果是原值，不减一的话，结果就成了cap * 2
        var n = cap - 1
        // 从最高位的1往低位复制
        n = n or n.ushr(1)
        n = n or n.ushr(2)
        n = n or n.ushr(4)
        n = n or n.ushr(8)
        n = n or n.ushr(16)
        // 到这里，从最高位的1到第0位都是1了，再加上1就是2的次幂
        return if (n < 0) 1 else if (n >= MAXIMUM_CAPACITY) MAXIMUM_CAPACITY else n + 1
    }

    private fun shardingKeyToCollectionName(shardValue: Any): String {
        val hashCode = shardValue.hashCode()
        val tableSequence = hashCode and shardingCount - 1
        return parseSequenceToCollectionName(tableSequence)
    }

    fun parseSequenceToCollectionName(sequence: Int): String {
        return collectionName + "_" + sequence
    }

    private fun determineShardingColumn(): String {
        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        if (shardingKey.column.isNotEmpty()) {
            return shardingKey.column
        }
        val fieldAnnotation = AnnotationUtils.getAnnotation(shardingField, org.springframework.data.mongodb.core.mapping.Field::class.java)
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
        requireNotNull(shardingValue) { "sharding value can not be empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(query: Query): String {
        val shardingValue = determineCollectionName(query.queryObject)
        requireNotNull(shardingValue) { "sharding value can not empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(aggregation: Aggregation): String {
        var shardingValue: Any? = null
        val pipeline = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)
        for (document in pipeline) {
            if (document.containsKey("\$match")) {
                val subDocument = document["\$match"] as Document
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
                for (element in value as BasicDBList) {
                    determineCollectionName(element as Document)?.let { return it }
                }
            }
        }
        return null
    }

    companion object {
        private const val MAXIMUM_CAPACITY = 1 shl 10
        private val logger = LoggerFactory.getLogger(ShardingMongoDao::class.java)
    }
}
