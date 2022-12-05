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

package com.tencent.bkrepo.common.mongo.dao

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.MongoCollectionUtils.getPreferredCollectionName
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.lang.reflect.ParameterizedType

/**
 * mongo db 数据访问层抽象类
 */
abstract class AbstractMongoDao<E> : MongoDao<E> {

    /**
     * 实体类Class
     */
    @Suppress("UNCHECKED_CAST")
    protected open val classType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>

    /**
     * 集合名称
     */
    protected open val collectionName: String by lazy { determineCollectionName() }

    fun findOne(query: Query): E? {
        return findOne(query, classType)
    }

    fun find(query: Query): List<E> {
        return find(query, classType)
    }

    fun findAll(): List<E> {
        return findAll(classType)
    }

    override fun <T> findOne(query: Query, clazz: Class<T>): T? {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao findOne: [$query] [$clazz]")
        }
        return determineMongoTemplate().findOne(query, clazz, determineCollectionName(query))
    }

    override fun <T> find(query: Query, clazz: Class<T>): List<T> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao find: [$query]")
        }
        return determineMongoTemplate().find(query, clazz, determineCollectionName(query))
    }

    override fun <T> findAll(clazz: Class<T>): List<T> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao find all")
        }
        return determineMongoTemplate().findAll(clazz, collectionName)
    }

    override fun insert(entity: E): E {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao insert: [$entity]")
        }
        return determineMongoTemplate().insert(entity, determineCollectionName(entity))
    }

    override fun insert(entityCollection: Collection<E>): Collection<E> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao insert many: [$entityCollection]")
        }
        return determineMongoTemplate().insert(entityCollection, collectionName)
    }

    override fun save(entity: E): E {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao save: [$entity]")
        }
        return determineMongoTemplate().save(entity, determineCollectionName(entity))
    }

    override fun remove(query: Query): DeleteResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao remove: [$query]")
        }
        return determineMongoTemplate().remove(query, classType, determineCollectionName(query))
    }

    override fun updateFirst(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao updateFirst: [$query], [$update]")
        }
        return determineMongoTemplate().updateFirst(query, update, determineCollectionName(query))
    }

    override fun updateMulti(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao updateMulti: [$query], [$update]")
        }
        return determineMongoTemplate().updateMulti(query, update, determineCollectionName(query))
    }

    override fun upsert(query: Query, update: Update): UpdateResult {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao upsert: [$query], [$update]")
        }
        val mongoTemplate = determineMongoTemplate()
        val collectionName = determineCollectionName(query)
        return try {
            mongoTemplate.upsert(query, update, collectionName)
        } catch (exception: DuplicateKeyException) {
            // retry because upsert operation is not atomic
            logger.warn("Upsert error[DuplicateKeyException]: " + exception.message.orEmpty())
            determineMongoTemplate().upsert(query, update, collectionName)
        }
    }

    override fun count(query: Query): Long {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao count: [$query]")
        }
        return determineMongoTemplate().count(query, determineCollectionName(query))
    }

    override fun exists(query: Query): Boolean {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao exists: [$query]")
        }
        return determineMongoTemplate().exists(query, determineCollectionName(query))
    }

    override fun <O> aggregate(aggregation: Aggregation, outputType: Class<O>): AggregationResults<O> {
        if (logger.isDebugEnabled) {
            logger.debug("Mongo Dao aggregate: [$aggregation], outputType: [$outputType]")
        }
        return determineMongoTemplate().aggregate(aggregation, determineCollectionName(aggregation), outputType)
    }

    protected open fun determineCollectionName(): String {
        var collectionName: String? = null
        if (classType.isAnnotationPresent(Document::class.java)) {
            val document = classType.getAnnotation(Document::class.java)
            collectionName = if (document.collection.isNotBlank()) document.collection else document.value
        }

        return if (collectionName.isNullOrEmpty()) getPreferredCollectionName(classType) else collectionName
    }

    abstract fun determineMongoTemplate(): MongoTemplate

    abstract fun determineCollectionName(entity: E): String

    abstract fun determineCollectionName(query: Query): String

    abstract fun determineCollectionName(aggregation: Aggregation): String

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AbstractMongoDao::class.java)

        /**
         * mongodb 默认id字段
         */
        const val ID = "_id"
    }
}
