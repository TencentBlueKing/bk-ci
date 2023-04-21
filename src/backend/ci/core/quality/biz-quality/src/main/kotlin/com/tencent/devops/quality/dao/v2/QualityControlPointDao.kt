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

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.quality.tables.TQualityControlPoint
import com.tencent.devops.model.quality.tables.records.TQualityControlPointRecord
import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import com.tencent.devops.quality.api.v2.pojo.op.ControlPointUpdate
import com.tencent.devops.quality.pojo.po.ControlPointPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
@Suppress("ALL")
class QualityControlPointDao {

    fun list(
        dslContext: DSLContext,
        elementType: Collection<String>
    ): List<TQualityControlPointRecord> {
        // remove logic to service
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            return dslContext.selectFrom(this)
                .where(ELEMENT_TYPE.`in`(elementType))
                .fetch()
        }
    }

    // 分页暂时不支持区分测试项目
    fun list(page: Int, pageSize: Int, dslContext: DSLContext): Result<TQualityControlPointRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            return dslContext.selectFrom(this)
                .where((TAG.isNull).or(TAG.ne("IN_READY_TEST")))
                .orderBy(CREATE_TIME.desc())
                .limit(sqlLimit.offset, sqlLimit.limit)
                .fetch()
        }
    }

    fun listAllControlPoint(dslContext: DSLContext): List<TQualityControlPointRecord> {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            return dslContext.selectFrom(this)
                .fetch()
                .filter { it.testProject.isNullOrBlank() }
        }
    }

    // 统计暂时不支持区分测试项目
    fun count(dslContext: DSLContext): Long {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            return dslContext.selectCount().from(this)
                .where((TAG.isNull).or(TAG.ne("IN_READY_TEST")))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun update(userId: String, id: Long, controlPointUpdate: ControlPointUpdate, dslContext: DSLContext): Long {
        return with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            val update = dslContext.update(this)

            with(controlPointUpdate) {
                if (!elementType.isNullOrBlank()) update.set(ELEMENT_TYPE, elementType)
                if (!name.isNullOrBlank()) update.set(NAME, name)
                if (!stage.isNullOrBlank()) update.set(STAGE, stage)
                if (!availablePosition.isNullOrBlank()) update.set(AVAILABLE_POSITION, availablePosition)
                if (!defaultPosition.isNullOrBlank()) update.set(DEFAULT_POSITION, defaultPosition)
                if (enable != null) update.set(ENABLE, enable)
            }
            update.set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute().toLong()
        }
    }

    // 暂时不支持区分测试项目
    fun getStages(dslContext: DSLContext): Result<Record1<String>> {
        return with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.select(STAGE).from(this)
                .where(STAGE.isNotNull.and(TAG.ne("IN_READY_TEST")))
                .groupBy(STAGE)
                .fetch()
        }
    }

    /**
     * 返回的第一个字段是elementType，第二个字段是elementName
     */
    // 暂时不支持区分测试项目
    fun getElementNames(dslContext: DSLContext): Result<Record2<String, String>> {
        return with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.select(ELEMENT_TYPE, NAME).from(this)
                .where(NAME.isNotNull.and(TAG.ne("IN_READY_TEST")))
                .groupBy(ELEMENT_TYPE, NAME)
                .fetch()
        }
    }

    fun setTestControlPoint(dslContext: DSLContext, userId: String, controlPoint: QualityControlPoint): Long {
        var pointId = 0L
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            val testControlPoint = dslContext.selectFrom(this)
                .where(ELEMENT_TYPE.eq(controlPoint.type).and(TAG.eq("IN_READY_TEST")))
                .fetchOne()
            if (testControlPoint != null) {
                dslContext.update(this)
                    .set(ELEMENT_TYPE, controlPoint.type)
                    .set(NAME, controlPoint.name)
                    .set(STAGE, controlPoint.stage)
                    .set(AVAILABLE_POSITION, controlPoint.availablePos.joinToString(",") { it.name })
                    .set(DEFAULT_POSITION, controlPoint.defaultPos.name)
                    .set(ENABLE, controlPoint.enable)
                    .set(UPDATE_USER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(testControlPoint.id))
                    .execute()
            } else {
                pointId = dslContext.insertInto(
                    this,
                    ELEMENT_TYPE,
                    NAME,
                    STAGE,
                    AVAILABLE_POSITION,
                    DEFAULT_POSITION,
                    ENABLE,
                    CREATE_USER,
                    UPDATE_USER,
                    CREATE_TIME,
                    UPDATE_TIME,
                    ATOM_VERSION,
                    TEST_PROJECT,
                    TAG
                )
                    .values(
                        controlPoint.type,
                        controlPoint.name,
                        controlPoint.stage,
                        controlPoint.availablePos.joinToString(",") { it.name },
                        controlPoint.defaultPos.name,
                        controlPoint.enable,
                        userId,
                        userId,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        controlPoint.atomVersion,
                        controlPoint.testProject,
                        "IN_READY_TEST"
                    ).returning(ID).fetchOne()!!.id
                val hashId = HashUtil.encodeLongId(pointId)
                dslContext.update(this)
                    .set(CONTROL_POINT_HASH_ID, hashId)
                    .where(ID.eq(pointId))
                    .execute()
            }
        }
        return pointId
    }

    fun refreshControlPoint(dslContext: DSLContext, elementType: String): Int {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                val controlPoints = transactionContext.selectFrom(this)
                    .where(ELEMENT_TYPE.eq(elementType))
                    .fetch()

                val testControlPoint = controlPoints.firstOrNull { it.tag == "IN_READY_TEST" }
                val prodControlPoint = controlPoints.firstOrNull { it.tag != "IN_READY_TEST" }

                // 测试为空，代表quality.json被删了，直接把生产的也删了
                if (testControlPoint == null) {
//                    transactionContext.deleteFrom(this)
//                        .where(ELEMENT_TYPE.eq(elementType))
//                        .execute()
                    return@transaction
                }

                if (prodControlPoint != null) {
                    transactionContext.update(this)
                        .set(NAME, testControlPoint.name)
                        .set(STAGE, testControlPoint.stage)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .where(ID.eq(prodControlPoint.id))
                        .execute()
                } else {
                    transactionContext.update(this)
                        .set(TAG, "IN_READY_RUNNING")
                        .set(TEST_PROJECT, "")
                        .where(ID.eq(testControlPoint.id))
                        .execute()
                }
            }
        }
        return 0
    }

    fun deleteTestControlPoint(dslContext: DSLContext, elementType: String): Int {
        return with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.deleteFrom(this)
                .where(ELEMENT_TYPE.eq(elementType).and(TAG.eq("IN_READY_TEST")))
                .execute()
        }
    }

    fun deleteControlPoint(dslContext: DSLContext, id: Long): Int {
        return with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getAllControlPoint(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<Record1<Long>>? {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            return dslContext.select(ID).from(this)
                .orderBy(CREATE_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateHashId(
        dslContext: DSLContext,
        id: Long,
        hashId: String
    ) {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.update(this)
                .set(CONTROL_POINT_HASH_ID, hashId)
                .where(ID.eq(id))
                .and(CONTROL_POINT_HASH_ID.isNull)
                .execute()
        }
    }

    fun batchCrateControlPoint(dslContext: DSLContext, controlPointPOs: List<ControlPointPO>) {
        with(TQualityControlPoint.T_QUALITY_CONTROL_POINT) {
            dslContext.batch(
                controlPointPOs.map { controlPointPO ->
                    dslContext.insertInto(this)
                        .set(dslContext.newRecord(this, controlPointPO))
                        .onDuplicateKeyUpdate()
                        .set(NAME, controlPointPO.name)
                        .set(STAGE, controlPointPO.stage)
                        .set(UPDATE_TIME, LocalDateTime.now())
                }
            ).execute()
        }
    }
}
