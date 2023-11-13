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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.quality.tables.records.TQualityControlPointRecord
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointData
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointUpdate
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.constant.QUALITY_CONTROL_POINT_NAME_KEY
import com.tencent.devops.quality.constant.QUALITY_CONTROL_POINT_STAGE_KEY
import com.tencent.devops.quality.dao.v2.QualityControlPointDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.pojo.po.ControlPointPO
import com.tencent.devops.quality.util.ElementUtils
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class QualityControlPointService @Autowired constructor(
    private val dslContext: DSLContext,
    private val controlPointDao: QualityControlPointDao,
    private val qualityRuleDao: QualityRuleDao,
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val redisOperation: RedisOperation,
    private val commonConfig: CommonConfig
) {

    @PostConstruct
    fun init() {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "QUALITY_CONTROL_POINT_INIT_LOCK",
            expiredTimeInSeconds = 60

        )
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init quality control point")
                    val classPathResource = ClassPathResource(
                        "i18n${File.separator}controlPoint_${commonConfig.devopsDefaultLocaleLanguage}.json"
                    )
                    val inputStream = classPathResource.inputStream
                    val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val controlPointPOs = JsonUtil.to(json, object : TypeReference<List<ControlPointPO>>() {})
                    controlPointDao.batchCrateControlPoint(dslContext, controlPointPOs)
                    logger.info("init quality control point end")
                } catch (ignored: Throwable) {
                    logger.warn("init quality control point fail! error:${ignored.message}")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }
    fun userGetByType(projectId: String, elementType: String?): QualityControlPoint? {
        return serviceGetByType(projectId, elementType)
    }

    fun serviceListByElementType(elementType: String): List<TQualityControlPointRecord> {
        return controlPointDao.list(dslContext, setOf(elementType))
    }

    fun serviceListFilter(
        controlPointRecords: List<TQualityControlPointRecord>,
        projectId: String
    ): List<TQualityControlPointRecord>? {
        val filterResult = mutableListOf<TQualityControlPointRecord>()
        // 获取生产跑的，或者测试项目对应的
        controlPointRecords.groupBy { it.elementType }.forEach { elementType, list ->
            val testControlPoint = list.firstOrNull { it.testProject == projectId }
            val prodControlPoint = list.firstOrNull { it.testProject.isNullOrBlank() }
            if (testControlPoint != null) {
                filterResult.add(testControlPoint)
            } else {
                if (prodControlPoint != null) filterResult.add(prodControlPoint)
            }
        }
        return filterResult
    }

    fun serviceGet(
        controlPointRecords: List<TQualityControlPointRecord>,
        projectId: String
    ): TQualityControlPointRecord? {
        return serviceListFilter(controlPointRecords, projectId)?.firstOrNull()
    }

    fun serviceGetByType(projectId: String, elementType: String?): QualityControlPoint? {
        if (elementType.isNullOrBlank()) return null
        val recordList = serviceListByElementType(elementType!!)
        val record = serviceGet(recordList, projectId) ?: return null
        return QualityControlPoint(
            hashId = HashUtil.encodeLongId(record.id ?: 0L),
            type = record.elementType ?: "",
            name = I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_CONTROL_POINT_NAME_KEY.format(record.elementType),
                defaultMessage = record.name ?: ""
            ),
            stage = I18nUtil.getCodeLanMessage(
                messageCode = QUALITY_CONTROL_POINT_STAGE_KEY.format(record.elementType),
                defaultMessage = record.stage ?: ""
            ),
            availablePos = if (record.availablePosition.isNullOrBlank()) {
                listOf()
            } else {
                record.availablePosition.split(",").map { name -> ControlPointPosition.create(name) }
            },
            defaultPos = ControlPointPosition.create(record.defaultPosition ?: ""),
            enable = record.enable ?: true,
            atomVersion = record.atomVersion
        )
    }

    fun userList(userId: String, projectId: String): List<QualityControlPoint> {
        return serviceList(projectId)
    }

    fun serviceList(projectId: String): List<QualityControlPoint> {
        val elements = ElementUtils.getProjectElement(projectId).keys
        val recordList = controlPointDao.list(dslContext, elements)
        val controlPointList = serviceListFilter(recordList, projectId) ?: return listOf()
        return controlPointList.filter { it.elementType in elements }
            .map {
                QualityControlPoint(
                    hashId = HashUtil.encodeLongId(it.id),
                    type = it.elementType,
                    name = I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_CONTROL_POINT_NAME_KEY.format(it.elementType),
                        defaultMessage = it.name
                    ),
                    stage = I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_CONTROL_POINT_STAGE_KEY.format(it.elementType),
                        defaultMessage = it.stage
                    ),
                    availablePos = it.availablePosition.split(",")
                        .map { name -> ControlPointPosition.create(name) },
                    defaultPos = ControlPointPosition.create(it.defaultPosition),
                    enable = it.enable,
                    atomVersion = it.atomVersion
                )
            }
    }

    fun listAllControlPoint(): List<TQualityControlPointRecord> {
        return controlPointDao.listAllControlPoint(dslContext)
    }

    fun opList(userId: String, page: Int, pageSize: Int): Page<ControlPointData> {
        val data = controlPointDao.list(page, pageSize, dslContext).map {
            ControlPointData(
                id = it.id,
                elementType = it.elementType,
                name = it.name,
                stage = it.stage,
                availablePosition = it.availablePosition,
                defaultPosition = it.defaultPosition,
                enable = it.enable
            )
        }
        val count = controlPointDao.count(dslContext)
        return Page<ControlPointData>(page, pageSize, count, data)
    }

    fun opUpdate(userId: String, id: Long, controlPointUpdate: ControlPointUpdate): Boolean {
        logger.info(
            "user($userId) update control point($id): ${controlPointUpdate.elementType}, " +
                "stage: ${controlPointUpdate.stage}"
        )
        if (controlPointDao.update(userId, id, controlPointUpdate, dslContext) > 0) {
            return true
        }
        return false
    }

    fun opGetStages(): List<String> {
        return this.controlPointDao.getStages(dslContext).map { it.value1() }
    }

    fun opGetElementNames(): List<ElementNameData> {
        return this.controlPointDao.getElementNames(dslContext).map {
            ElementNameData(it.value1(), it.value2())
        }
    }

    fun isControlPoint(elementType: String, atomVersion: String, projectId: String): Boolean {
        val recordList = serviceListByElementType(elementType)
        val controlPoint = serviceGet(recordList, projectId)
        return controlPoint != null && controlPoint.atomVersion <= atomVersion
    }

    fun setTestControlPoint(userId: String, controlPoint: QualityControlPoint): Long {
        logger.info("QUALITY|setTestControlPoint userId: $userId, controlPoint: ${controlPoint.type}")
        return controlPointDao.setTestControlPoint(dslContext, userId, controlPoint)
    }

    fun refreshControlPoint(elementType: String): Int {
        logger.info("QUALITY|refreshControlPoint controlPoint: $elementType")
        return controlPointDao.refreshControlPoint(dslContext, elementType)
    }

    fun deleteTestControlPoint(elementType: String): Int {
        logger.info("QUALITY|deleteTestControlPoint controlPoint: $elementType")
        return controlPointDao.deleteTestControlPoint(dslContext, elementType)
    }

    fun deleteControlPoint(id: Long): Int {
        return controlPointDao.deleteControlPoint(dslContext, id)
    }

    fun addHashId() {
        val startTime = System.currentTimeMillis()
        logger.info("QualityControlPointService:begin addHashId-----------")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("QualityControlPointService:begin addHashId threadPoolExecutor-----------")
            var offset = 0
            val limit = 1000
            try {
                do {
                    val controlPointRecords = controlPointDao.getAllControlPoint(dslContext, limit, offset)
                    val controlPointSize = controlPointRecords?.size
                    logger.info("controlPointSize:$controlPointSize")
                    controlPointRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        controlPointDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (controlPointSize == 1000)
                offset = 0
                do {
                    val ruleRecords = qualityRuleDao.getAllRule(dslContext, limit, offset)
                    val ruleSize = ruleRecords?.size
                    logger.info("ruleSize:$ruleSize")
                    ruleRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        qualityRuleDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (ruleSize == 1000)
                offset = 0
                do {
                    val ruleBuildHisRecords = qualityRuleBuildHisDao.getAllRuleBuildHis(dslContext, limit, offset)
                    val ruleBuildHisSize = ruleBuildHisRecords?.size
                    logger.info("ruleBuildHisSize:$ruleBuildHisSize")
                    ruleBuildHisRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeLongId(it.value1())
                        qualityRuleBuildHisDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (ruleBuildHisSize == 1000)
            } catch (e: Exception) {
                logger.warn("QualityControlPointService：addHashId failed | $e ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
        logger.info("QualityControlPointService:finish addHashId-----------")
        logger.info("addhashid time cost: ${System.currentTimeMillis() - startTime}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityControlPointService::class.java)
    }
}
