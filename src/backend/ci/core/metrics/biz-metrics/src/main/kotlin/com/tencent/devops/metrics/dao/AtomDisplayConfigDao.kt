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

package com.tencent.devops.metrics.dao

import com.tencent.devops.model.metrics.tables.TAtomDisplayConfig
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.po.AtomDisplayConfigPO
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AtomDisplayConfigDao {

    fun batchAddAtomDisplayConfig(
        dslContext: DSLContext,
        atomDisplayConfigPOS: List<AtomDisplayConfigPO>
    ) {
        with(TAtomDisplayConfig.T_ATOM_DISPLAY_CONFIG) {
            atomDisplayConfigPOS.forEach { atomDisplayConfigPO ->
                dslContext.insertInto(this)
                    .set(ID, atomDisplayConfigPO.id)
                    .set(PROJECT_ID, atomDisplayConfigPO.projectId)
                    .set(ATOM_CODE, atomDisplayConfigPO.atomCode)
                    .set(ATOM_NAME, atomDisplayConfigPO.atomName)
                    .set(CREATOR, atomDisplayConfigPO.userId)
                    .set(MODIFIER, atomDisplayConfigPO.userId)
                    .set(UPDATE_TIME, atomDisplayConfigPO.updateTime)
                    .set(CREATE_TIME, atomDisplayConfigPO.createTime)
                    .onDuplicateKeyUpdate()
                    .set(ATOM_NAME, atomDisplayConfigPO.atomName)
                    .set(MODIFIER, atomDisplayConfigPO.userId)
                    .set(UPDATE_TIME, atomDisplayConfigPO.updateTime)
                    .execute()
            }
        }
    }

    fun batchDeleteAtomDisplayConfig(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        atomCodes: List<String>
    ): Int {
        with(TAtomDisplayConfig.T_ATOM_DISPLAY_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ATOM_CODE.`in`(atomCodes))
                .execute()
        }
    }

    fun getAtomDisplayConfig(
        dslContext: DSLContext,
        projectId: String,
        keyword: String?
    ): List<AtomBaseInfoDO> {
        with(TAtomDisplayConfig.T_ATOM_DISPLAY_CONFIG) {
            val step = dslContext.select(ATOM_CODE, ATOM_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
            val conditionStep =
                if (!keyword.isNullOrBlank()) {
                    step.and(ATOM_NAME.like("%$keyword%"))
                } else { step }
                return conditionStep.fetchInto(AtomBaseInfoDO::class.java)
        }
    }

    fun getOptionalAtomDisplayConfig(
        dslContext: DSLContext,
        projectId: String,
        atomCodes: List<String>?,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): List<AtomBaseInfoDO> {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!atomCodes.isNullOrEmpty()) {
                conditions.add(ATOM_CODE.notIn(atomCodes))
            }
            if (!keyword.isNullOrBlank()) {
                conditions.add(ATOM_NAME.like("%$keyword%"))
            }
            val step = dslContext.select(ATOM_CODE, ATOM_NAME).from(this)
                .where(conditions)
                return step.groupBy(ATOM_CODE)
                    .orderBy(SUCCESS_RATE)
                    .limit((page - 1) * pageSize, pageSize)
                    .fetchInto(AtomBaseInfoDO::class.java)
        }
    }

    fun getOptionalAtomDisplayConfigCount(
        dslContext: DSLContext,
        projectId: String,
        atomCodes: List<String>,
        keyword: String?
    ): Long {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val step = dslContext.selectDistinct(ATOM_CODE)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ATOM_CODE.notIn(atomCodes))
                if (!keyword.isNullOrBlank()) {
                    step.and(ATOM_NAME.like("%$keyword%"))
                }
            return step.execute().toLong()
        }
    }
}
