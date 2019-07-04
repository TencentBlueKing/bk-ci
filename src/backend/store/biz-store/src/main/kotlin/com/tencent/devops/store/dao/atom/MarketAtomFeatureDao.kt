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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.records.TAtomFeatureRecord
import com.tencent.devops.store.pojo.atom.AtomFeatureRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MarketAtomFeatureDao {

    /**
     * 添加插件插件特性
     */
    fun addAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: AtomFeatureRequest) {
        with(TAtomFeature.T_ATOM_FEATURE) {
            dslContext.insertInto(
                this,
                ID,
                ATOM_CODE,
                VISIBILITY_LEVEL,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomFeatureRequest.atomCode,
                    atomFeatureRequest.visibilityLevel,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取插件插件特性
     */
    fun getAtomFeature(dslContext: DSLContext, atomCode: String): TAtomFeatureRecord? {
        with(TAtomFeature.T_ATOM_FEATURE) {
            return dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .fetchOne()
        }
    }

    /**
     * 更新插件插件特性
     */
    fun updateAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: AtomFeatureRequest) {
        with(TAtomFeature.T_ATOM_FEATURE) {
            val record = dslContext.selectFrom(this).where(ATOM_CODE.eq(atomFeatureRequest.atomCode)).fetchOne()
            if (null == record) {
                addAtomFeature(dslContext, userId, atomFeatureRequest)
            } else {
                val baseStep = dslContext.update(this)
                val visibilityLevel = atomFeatureRequest.visibilityLevel
                if (null != visibilityLevel) {
                    baseStep.set(VISIBILITY_LEVEL, visibilityLevel)
                }
                baseStep.set(MODIFIER, userId)
                    .where(ATOM_CODE.eq(atomFeatureRequest.atomCode))
                    .execute()
            }
        }
    }
}