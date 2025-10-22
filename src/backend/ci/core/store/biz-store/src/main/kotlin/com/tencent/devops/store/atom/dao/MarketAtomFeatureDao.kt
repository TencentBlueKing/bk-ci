/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.records.TAtomFeatureRecord
import com.tencent.devops.store.pojo.atom.AtomFeatureRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MarketAtomFeatureDao {

    /**
     * 添加插件特性
     */
    fun addAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: AtomFeatureRequest) {
        with(TAtomFeature.T_ATOM_FEATURE) {
            dslContext.insertInto(this,
                ID,
                ATOM_CODE,
                RECOMMEND_FLAG,
                YAML_FLAG,
                QUALITY_FLAG,
                CERTIFICATION_FLAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomFeatureRequest.atomCode,
                    atomFeatureRequest.recommendFlag,
                    atomFeatureRequest.yamlFlag,
                    atomFeatureRequest.qualityFlag,
                    atomFeatureRequest.certificationFlag,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取插件特性
     */
    fun getAtomFeature(dslContext: DSLContext, atomCode: String): TAtomFeatureRecord? {
        with(TAtomFeature.T_ATOM_FEATURE) {
            return dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .fetchOne()
        }
    }

    /**
     * 更新插件特性
     */
    fun updateAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: AtomFeatureRequest) {
        with(TAtomFeature.T_ATOM_FEATURE) {
            val record = dslContext.selectFrom(this).where(ATOM_CODE.eq(atomFeatureRequest.atomCode)).fetchOne()
            if (null == record) {
                addAtomFeature(dslContext, userId, atomFeatureRequest)
            } else {
                val baseStep = dslContext.update(this)
                val recommendFlag = atomFeatureRequest.recommendFlag
                if (null != recommendFlag) {
                    baseStep.set(RECOMMEND_FLAG, recommendFlag)
                }
                val deleteFlag = atomFeatureRequest.deleteFlag
                if (null != deleteFlag) {
                    baseStep.set(DELETE_FLAG, deleteFlag)
                }
                val yamlFlag = atomFeatureRequest.yamlFlag
                if (null != yamlFlag) {
                    baseStep.set(YAML_FLAG, yamlFlag)
                }
                val qualityFlag = atomFeatureRequest.qualityFlag
                if (null != qualityFlag) {
                    baseStep.set(QUALITY_FLAG, qualityFlag)
                }
                val certificationFlag = atomFeatureRequest.certificationFlag
                if (null != certificationFlag) {
                    baseStep.set(CERTIFICATION_FLAG, certificationFlag)
                }
                baseStep.set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ATOM_CODE.eq(atomFeatureRequest.atomCode))
                    .execute()
            }
        }
    }

    fun deleteAtomFeature(dslContext: DSLContext, atomCode: String) {
        with(TAtomFeature.T_ATOM_FEATURE) {
            dslContext.deleteFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }
}
