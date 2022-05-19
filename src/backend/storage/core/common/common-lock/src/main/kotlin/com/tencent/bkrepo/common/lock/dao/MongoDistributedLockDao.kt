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

package com.tencent.bkrepo.common.lock.dao

import com.tencent.bkrepo.common.lock.model.TMongoDistributedLock
import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository

/**
 * mongodb分布式锁数据访问层
 */
@Repository
class MongoDistributedLockDao : SimpleMongoDao<TMongoDistributedLock>() {
    /**
     * 根据key查找对应锁
     */
    fun findByKey(key: String): TMongoDistributedLock? {
        return this.findOne(Query(TMongoDistributedLock::key.isEqualTo(key)))
    }

    /**
     * 根据key释放对应锁
     */
    fun deleteByKey(key: String) {
        if (key.isNotBlank()) {
            this.remove(Query(TMongoDistributedLock::key.isEqualTo(key)))
        }
    }

    /**
     * 删除过期的key
     */
    fun deleteExpireKey(key: String, expireTime: Long) {
        val criteria = where(TMongoDistributedLock::key).isEqualTo(key)
            .and(TMongoDistributedLock::expireTime.name).lt(expireTime)
        this.remove(Query(criteria))
    }

    /**
     * 指定key自增,并设置过期时间
     */
    fun incrByKeyWithExpire(key: String, increase: Int, expireTime: Long): TMongoDistributedLock? {
        val criteria = where(TMongoDistributedLock::key).isEqualTo(key)
        val query = Query(criteria)
        val update = Update().inc(TMongoDistributedLock::value.name, increase)
            .set(TMongoDistributedLock::expireTime.name, expireTime)
        val options = FindAndModifyOptions().apply { this.upsert(true).returnNew(true) }
        return determineMongoTemplate()
            .findAndModify(query, update, options, TMongoDistributedLock::class.java)
    }
}
