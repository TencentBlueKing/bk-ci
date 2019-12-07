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

package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.quality.tables.records.TQualityMetadataRecord
import com.tencent.devops.quality.api.v2.pojo.QualityIndicatorMetadata
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.dao.v2.QualityMetadataDao
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityMetadataService @Autowired constructor(
    val dslContext: DSLContext,
    val metadataDao: QualityMetadataDao
) {
    fun serviceListMetadata(metadataIds: Collection<Long>): List<QualityIndicatorMetadata> {
        return metadataDao.list(dslContext, metadataIds)?.map {
            QualityIndicatorMetadata(
                    HashUtil.encodeLongId(it.id),
                    it.dataId,
                    it.dataName,
                    it.elementType,
                    it.elementName,
                    it.elementDetail,
                    QualityDataType.valueOf(it.valueType),
                    it.desc,
                    it.extra
            )
        }?.toList() ?: listOf()
    }

    fun opList(
        elementName: String?,
        elementDetail: String?,
        searchString: String?,
        page: Int?,
        pageSize: Int?
    ): Page<QualityMetaData> {
        val data = metadataDao.list(elementName, elementDetail, searchString, page!!, pageSize!!, dslContext)
        val resultData = data.map {
            QualityMetaData(
                    it.id, it.dataId, it.dataName, it.elementType, it.elementName, it.elementDetail,
                    it.valueType, it.desc, it.extra
            )
        }
        val count = metadataDao.count(elementName, elementDetail, searchString, dslContext)

        return Page(page, pageSize, count, resultData)
    }

    fun serviceListByDataId(elementType: String, dataIds: Collection<String>): List<QualityIndicatorMetadata> {
        return metadataDao.listByDataId(dslContext, elementType, dataIds)?.map {
            QualityIndicatorMetadata(
                    HashUtil.encodeLongId(it.id),
                    it.dataId,
                    it.dataName,
                    it.elementType,
                    it.elementName,
                    it.elementDetail,
                    QualityDataType.valueOf(it.valueType),
                    it.desc,
                    it.extra
            )
        }?.toList() ?: listOf()
    }

    fun opGetElementNames(): List<ElementNameData> {
        return this.metadataDao.getElementNames(dslContext).map {
            ElementNameData(it.value1(), it.value2())
        }
    }

    fun opGetElementDetails(): List<String> {
        return metadataDao.getElementDetails(dslContext).map { it.value1() }
    }

    fun serviceListByIds(metadataIds: Set<Long>): Result<TQualityMetadataRecord> {
        return metadataDao.listByIds(metadataIds, dslContext)
    }

    fun serviceSetTestMetadata(userId: String, elementType: String, metadataList: List<QualityMetaData>): Map<String, Long> {
        val data = metadataDao.listByElementType(dslContext, elementType)?.filter { it.extra == "IN_READY_TEST" }
        val lastMetadataIdMap = data?.map { it.dataId to it.id }?.toMap() ?: mapOf()
        val newDataId = metadataList.map { it.dataId!! }
        val lastDataId = lastMetadataIdMap.keys

        // 这次没有的就删掉
        metadataDao.delete(lastDataId.minus(newDataId).map { lastMetadataIdMap[it]!! }, dslContext)

        // 有则update，没则insert
        return metadataList.map {
            if (lastMetadataIdMap.contains(it.dataId)) {
                val id = lastMetadataIdMap[it.dataId]!!
                metadataDao.update(userId, id, it, dslContext)
                it.dataId!! to id
            } else {
                val id = metadataDao.insert(userId, it, dslContext)
                it.dataId!! to id
            }
        }.toMap()
    }

    // 把测试的数据刷到正式的， 有则update，没也update，多余的删掉
    fun serviceRefreshMetadata(elementType: String): Map<String, String> {
        val data = metadataDao.listByElementType(dslContext, elementType)
        val testData = data?.filter { it.extra == "IN_READY_TEST" } ?: listOf()
        val prodData = data?.filter { it.extra != "IN_READY_TEST" } ?: listOf()
        val userId = testData.firstOrNull()?.createUser ?: ""

        val resultMap = mutableMapOf<String, String>()

        // 有则update
        val deleteItemId = mutableSetOf<Long>()
        prodData.forEach PROD@{ prodItem ->
            testData.forEach TEST@{ testItem ->
                if (prodItem.dataId == testItem.dataId) {
                    metadataDao.update(userId, prodItem.id, QualityMetaData(
                        testItem.id,
                        testItem.dataId,
                        testItem.dataName,
                        testItem.elementType,
                        testItem.elementName,
                        testItem.elementDetail,
                        testItem.valueType,
                        testItem.desc,
                        "IN_READY_RUNNING"
                    ), dslContext)
                    resultMap[prodItem.dataId] = prodItem.id.toString()
                    return@PROD
                }
            }

            // test没有的，多余的删掉
            deleteItemId.add(prodItem.id)
        }
        metadataDao.delete(deleteItemId, dslContext)

        // 没也update
        testData.forEach TEST@{ testItem ->
            prodData.forEach PROD@{ prodItem ->
                if (prodItem.dataId == testItem.dataId) return@TEST
            }
            val id = metadataDao.update(userId, testItem.id, QualityMetaData(
                testItem.id,
                testItem.dataId,
                testItem.dataName,
                testItem.elementType,
                testItem.elementName,
                testItem.elementDetail,
                testItem.valueType,
                testItem.desc,
                "IN_READY_RUNNING"
            ), dslContext)
            resultMap[testItem.dataId] = id.toString()
        }
        return resultMap
    }

    fun serviceDeleteTestMetadata(elementType: String): Int {
        val data = metadataDao.listByElementType(dslContext, elementType)
        val testData = data?.filter { it.extra == "IN_READY_TEST" } ?: listOf()
        return metadataDao.delete(testData.map { it.id }, dslContext)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityMetadataService::class.java)
    }
}
