/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.tencent.devops.model.quality.tables.records.TQualityControlPointRecord
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.dao.v2.QualityControlPointDao
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointData
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointUpdate
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.util.ElementUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityControlPointService @Autowired constructor(
    private val dslContext: DSLContext,
    private val controlPointDao: QualityControlPointDao
) {

    fun serviceGet(elementType: String): TQualityControlPointRecord? {
        return controlPointDao.get(dslContext, elementType)
    }

    fun userList(userId: String, projectId: String): List<QualityControlPoint> {
        return serviceList(projectId)
    }

    fun serviceList(projectId: String): List<QualityControlPoint> {
        val controlPointList = controlPointDao.list(dslContext) ?: return listOf()
        val elements = ElementUtils.getProjectElement(projectId).keys
        return controlPointList.filter { it.elementType in elements }.filter { it.testProject.isBlank() || (it.testProject == projectId) }
                .map {
            QualityControlPoint(
                    HashUtil.encodeLongId(it.id),
                    it.elementType,
                    it.name,
                    it.stage,
                    it.availablePosition.split(",").map { name -> ControlPointPosition(name) },
                    ControlPointPosition(it.defaultPosition),
                    it.enable,
                    it.atomVersion
            )
        }
    }

    fun userGetByType(elementType: String?): QualityControlPoint? {
        return serviceGetByType(elementType)
    }

    fun serviceGetByType(elementType: String?): QualityControlPoint? {
        if (elementType.isNullOrBlank()) return null
        val record = controlPointDao.getByType(dslContext, elementType!!) ?: return null
        return QualityControlPoint(
                HashUtil.encodeLongId(record.id ?: 0L),
                record.elementType ?: "",
                record.name ?: "",
                record.stage ?: "",
                if (record.availablePosition.isNullOrBlank()) listOf() else record.availablePosition.split(",").map { name -> ControlPointPosition(name) },
                ControlPointPosition(record.defaultPosition ?: ""),
                record.enable ?: true,
                record.atomVersion
        )
    }

    fun opList(userId: String, page: Int, pageSize: Int): Page<ControlPointData> {
        val data = controlPointDao.list(page, pageSize, dslContext).map {
            ControlPointData(
                    it.id,
                    it.elementType,
                    it.name,
                    it.stage,
                    it.availablePosition,
                    it.defaultPosition,
                    it.enable
            )
        }
        val count = controlPointDao.count(dslContext)
        return Page<ControlPointData>(page, pageSize, count, data)
    }

    fun opUpdate(userId: String, id: Long, controlPointUpdate: ControlPointUpdate): Boolean {
        logger.info("user($userId) update control point($id): $controlPointUpdate")
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

    fun serviceCreateOrUpdate(userId: String, controlPoint: QualityControlPoint): Int {
        return controlPointDao.serviceCreateOrUpdate(dslContext, userId, controlPoint)
    }

    fun isControlPoint(elementType: String, atomVersion: String): Boolean {
        val controlPoint = controlPointDao.get(dslContext, elementType)
        return controlPoint != null && controlPoint.atomVersion <= atomVersion
    }

    fun cleanTestProject(controlPointType: String): Int {
        return controlPointDao.cleanTestProject(dslContext, controlPointType)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityControlPointService::class.java)
    }
}
