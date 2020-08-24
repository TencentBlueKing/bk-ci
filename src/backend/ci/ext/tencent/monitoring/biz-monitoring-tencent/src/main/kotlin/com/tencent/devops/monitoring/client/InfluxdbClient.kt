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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.monitoring.client

import com.tencent.devops.monitoring.pojo.annotions.InfluxTag
import org.apache.commons.lang3.reflect.FieldUtils
import org.influxdb.BatchOptions
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Point
import org.influxdb.dto.Point.measurement
import org.influxdb.dto.Query
import org.influxdb.dto.QueryResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

@Component
class InfluxdbClient {
    companion object {
        private val logger = LoggerFactory.getLogger(InfluxdbClient::class.java)
    }

    @Value("\${influxdb.server:}")
    val influxdbServer: String = ""

    @Value("\${influxdb.userName:}")
    val influxdbUserName: String = ""

    @Value("\${influxdb.password:}")
    val influxdbPassword: String = ""

    private val dbName = "monitoring"
    private val monitoringRetentionPolicy = "monitoring_retention"
    private val atomInt = AtomicInteger()

    private lateinit var influxDB: InfluxDB

    @PostConstruct
    fun init() {
        influxDB = InfluxDBFactory.connect(influxdbServer, influxdbUserName, influxdbPassword)

        try {
            // 如果指定的数据库不存在，则新建一个新的数据库，并新建一个默认的数据保留规则
            if (!this.databaseExist(dbName)) {
                createDatabase()
                createRetentionPolicy()
            }
        } catch (e: Exception) {
            logger.error("Create influxdb failed:", e)
        } finally {
            influxDB.setRetentionPolicy(monitoringRetentionPolicy)
        }
        influxDB.setLogLevel(InfluxDB.LogLevel.NONE)
        influxDB.enableBatch(BatchOptions.DEFAULTS
            .actions(1000)
            .flushDuration(100)
            .bufferLimit(100)
            .jitterDuration(100)
            .exceptionHandler { points: Iterable<Point>, e: Throwable? ->
                try {
                    points.forEach { logger.error("failed to write point $it", e) }
                } catch (e: Exception) {
                    // Do nothing , 这个handler不能抛异常,否则influxdb批量插入的线程就会停止
                }
            }
            .threadFactory(
                Executors.defaultThreadFactory()
            ))
    }

    private fun createDatabase() {
        influxDB.query(Query("CREATE DATABASE $dbName", ""))
        influxDB.query(Query("CREATE USER $influxdbUserName WITH PASSWORD '$influxdbPassword'", ""))
        influxDB.query(Query("GRANT ALL PRIVILEGES ON $dbName TO $influxdbUserName", ""))
    }

    private fun createRetentionPolicy() {
        influxDB.query(
            Query(
                "CREATE RETENTION POLICY $monitoringRetentionPolicy ON $dbName DURATION 30d REPLICATION 1 DEFAULT",
                ""
            )
        )
    }

    private fun databaseExist(database: String?): Boolean {
        return influxDB.databaseExists(database)
    }

    fun insert(any: Any) {
        val (fields, tags) = getFieldTagMap(any)
        insert(any::class.java.simpleName, tags, fields)
    }

    fun insert(measurement: String, tags: Map<String, String>, fields: Map<String, Any>) {
        val builder: Point.Builder = measurement(measurement)
        builder.tag(tags)
        builder.fields(fields)
        builder.time(System.currentTimeMillis() * 1000000 + getTail() % 1000000, TimeUnit.NANOSECONDS)
        influxDB.write(dbName, monitoringRetentionPolicy, builder.build())
    }

    fun select(sql: String): QueryResult? {
        return influxDB.query(Query(sql, dbName))
    }

    private fun getFieldTagMap(any: Any): Pair<Map<String, Any>/*field*/, Map<String, String>/*tag*/> {
        val field: MutableMap<String, Any> = mutableMapOf()
        val tag: MutableMap<String, String> = mutableMapOf()

        FieldUtils.getAllFields(any.javaClass).forEach {
            it.isAccessible = true
            if (it.isAnnotationPresent(InfluxTag::class.java)) {
                tag[it.name] = it.get(any)?.toString() ?: ""
            } else {
                val value = it.get(any)
                field[it.name] = if (value == null) "" else {
                    if (value is Number) value else value.toString()
                }
            }
        }

        return field to tag
    }

    /**
     * 添加尾巴,避免被覆盖
     */
    private fun getTail(): Int {
        val tail = atomInt.incrementAndGet()
        return if (tail < 0) {
            if (atomInt.compareAndSet(tail, 0)) {
                0
            } else {
                atomInt.incrementAndGet()
            }
        } else {
            tail
        }
    }
}