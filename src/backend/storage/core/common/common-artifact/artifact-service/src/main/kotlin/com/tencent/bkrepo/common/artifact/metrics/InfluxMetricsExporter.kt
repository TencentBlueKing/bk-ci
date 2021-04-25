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

package com.tencent.bkrepo.common.artifact.metrics

import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.common.service.actuator.CommonTagProvider
import org.influxdb.InfluxDB
import org.influxdb.dto.BatchPoints
import org.influxdb.dto.Point
import org.influxdb.dto.Query
import org.influxdb.impl.InfluxDBImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxProperties
import java.util.Queue

class InfluxMetricsExporter(
    private val influxProperties: InfluxProperties,
    commonTagProvider: ObjectProvider<CommonTagProvider>
) {

    private val commonTags: Map<String, String>
    private val influxDB: InfluxDB

    init {
        logger.info("Initializing InfluxMetricsExporter")
        with(influxProperties) {
            val builder = HttpClientBuilderFactory.create()
            influxDB = InfluxDBImpl(uri, userName.orEmpty(), password.orEmpty(), builder)
            influxDB.setDatabase(db)
            influxDB.setRetentionPolicy(retentionPolicy)
            createDatabaseIfNecessary()
        }
        commonTags = commonTagProvider.ifAvailable?.provide().orEmpty()
    }

    fun export(queue: Queue<ArtifactTransferRecord>) {
        with(influxProperties) {
            if (!isEnabled) {
                return
            }
            val clazz = ArtifactTransferRecord::class.java
            val points = ArrayList<Point>()
            var record = queue.poll()
            while (record != null) {
                val point = Point.measurementByPOJO(clazz).addFieldsFromPOJO(record).tag(commonTags).build()
                points.add(point)
                record = queue.poll()
            }
            val batchPoints = BatchPoints.database(db).points(points).retentionPolicy(retentionPolicy).build()
            influxDB.write(batchPoints)
            logger.debug("Export [${points.size}] artifact transfer points to influxdb successfully")
        }
    }

    private fun createDatabaseIfNecessary() {
        if (influxProperties.isEnabled && influxProperties.isAutoCreateDb) {
            val createDatabaseQuery = CreateDatabaseQueryBuilder(influxProperties.db)
                .setRetentionDuration(influxProperties.retentionDuration)
                .setRetentionPolicyName(influxProperties.retentionPolicy)
                .setRetentionReplicationFactor(influxProperties.retentionReplicationFactor)
                .setRetentionShardDuration(influxProperties.retentionShardDuration)
                .build()
            influxDB.query(Query(createDatabaseQuery))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InfluxMetricsExporter::class.java)
    }
}
