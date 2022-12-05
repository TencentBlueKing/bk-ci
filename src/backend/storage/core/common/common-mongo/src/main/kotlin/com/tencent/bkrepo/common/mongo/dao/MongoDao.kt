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
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

/**
 * mongo db 数据访问层接口
 */
interface MongoDao<E> {

    /**
     * 通过查询对象查询单条文档，返回元素类型由clazz指定
     */
    fun <T> findOne(query: Query, clazz: Class<T>): T?

    /**
     * 通过查询对象查询文档集合，返回元素类型由clazz指定
     */
    fun <T> find(query: Query, clazz: Class<T>): List<T>

    /**
     * 通过查询对象查询文档集合，返回元素类型由clazz指定
     */
    fun <T> findAll(clazz: Class<T>): List<T>

    /**
     * 新增文档到数据库的集合中
     */
    fun insert(entity: E): E

    /**
     * 新增文档到数据库的集合中
     */
    fun insert(entityCollection: Collection<E>): Collection<E>

    /**
     * 新增文档到数据库的集合中
     */
    fun save(entity: E): E

    /**
     * 更新单条文档
     */
    fun updateFirst(query: Query, update: Update): UpdateResult

    /**
     * 更新文档
     */
    fun updateMulti(query: Query, update: Update): UpdateResult

    /**
     * update or insert
     */
    fun upsert(query: Query, update: Update): UpdateResult

    /**
     * 统计数量
     */
    fun count(query: Query): Long

    /**
     * 判断文档是否存在
     */
    fun exists(query: Query): Boolean

    /**
     * 删除文档
     */
    fun remove(query: Query): DeleteResult

    /**
     * 文档聚合操作
     */
    fun <O> aggregate(aggregation: Aggregation, outputType: Class<O>): AggregationResults<O>
}
