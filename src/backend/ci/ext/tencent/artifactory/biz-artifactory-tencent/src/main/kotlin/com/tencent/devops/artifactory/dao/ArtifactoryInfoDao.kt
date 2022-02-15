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

package com.tencent.devops.artifactory.dao

import com.tencent.devops.artifactory.Constants
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.artifactory.Tables.T_TIPELINE_ARTIFACETORY_INFO
import com.tencent.devops.model.artifactory.tables.TTipelineArtifacetoryInfo
import com.tencent.devops.model.artifactory.tables.records.TTipelineArtifacetoryInfoRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class ArtifactoryInfoDao {

    fun create(
        dslContext: DSLContext,
        fileInfo: FileInfo,
        pipelineId: String,
        buildId: String,
        buildNum: Int,
        projcetId: String,
        dataFrom: Int
    ): Long {

        with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            var bundleIdentifier = ""
            var appVersion = ""
            fileInfo.properties!!.forEach {
                if (it.key.equals("bundleIdentifier")) {
                    bundleIdentifier = it.value
                }
                if (it.key.equals("appVersion")) {
                    appVersion = it.value
                }
            }

            val record = dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_ID,
                PROJECT_ID,
                BUNDLE_ID,
                BUILD_NUM,
                NAME,
                FULL_NAME,
                PATH,
                FULL_PATH,
                SIZE,
                MODIFIED_TIME,
                ARTIFACTORY_TYPE,
                PROPERTIES,
                APP_VERSION,
                DATA_FROM
            ).values(
                pipelineId,
                buildId,
                projcetId,
                bundleIdentifier,
                buildNum,
                fileInfo.name,
                fileInfo.fullName,
                fileInfo.path,
                fileInfo.fullPath,
                fileInfo.size.toInt(),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(fileInfo.modifiedTime), ZoneId.systemDefault()),
                fileInfo.artifactoryType.toString(),
                JsonUtil.toJson(fileInfo.properties ?: emptyList<Property>()),
                appVersion,
                dataFrom.toByte()
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun searchAritfactoryInfo(
        dslContext: DSLContext,
        pipelineId: String,
        startTime: Long,
        endTime: Long
    ): Result<TTipelineArtifacetoryInfoRecord>? {
        return with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectFrom(this).where(
                PIPELINE_ID.eq(pipelineId)
            )
            if (startTime > 0) {
                where.and(
                    MODIFIED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            if (endTime > 0) {
                where.and(
                    MODIFIED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            where.orderBy(ID.asc())
                .fetch()
        }
    }

    fun getLastCompensateData(
        dslContext: DSLContext
    ): Result<TTipelineArtifacetoryInfoRecord> {
        return with(TTipelineArtifacetoryInfo.T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectFrom(this).where(
                DATA_FROM.eq(Constants.SYN_DATA_FROM_COMPENSATE.toByte())
            )
            where.orderBy(MODIFIED_TIME.desc()).limit(0, 1)

            where.fetch()
        }
    }

    fun selectCountByDataFrom(dslContext: DSLContext, dataForm: Int, startTime: Long, endTime: Long): Int {
        return with(T_TIPELINE_ARTIFACETORY_INFO) {
            val where = dslContext.selectDistinct(BUILD_ID).from(this).where(DATA_FROM.eq(dataForm.toByte()))

            if (startTime > 0) {
                where.and(
                    MODIFIED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            if (endTime > 0) {
                where.and(
                    MODIFIED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }

            where.count()
        }
    }
}
