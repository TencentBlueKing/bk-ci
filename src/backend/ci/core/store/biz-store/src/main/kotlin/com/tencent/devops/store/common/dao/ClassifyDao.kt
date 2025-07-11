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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.records.TClassifyRecord
import com.tencent.devops.store.pojo.common.classify.Classify
import com.tencent.devops.store.pojo.common.classify.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ClassifyDao {

    fun add(dslContext: DSLContext, id: String, classifyRequest: ClassifyRequest, type: Byte) {
        with(TClassify.T_CLASSIFY) {
            dslContext.insertInto(
                this,
                ID,
                CLASSIFY_CODE,
                CLASSIFY_NAME,
                WEIGHT,
                TYPE
            )
                .values(
                    id,
                    classifyRequest.classifyCode,
                    classifyRequest.classifyName,
                    classifyRequest.weight,
                    type
                ).execute()
        }
    }

    fun countByName(dslContext: DSLContext, classifyName: String, type: Byte): Int {
        with(TClassify.T_CLASSIFY) {
            return dslContext.selectCount().from(this).where(CLASSIFY_NAME.eq(classifyName).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, classifyCode: String, type: Byte): Int {
        with(TClassify.T_CLASSIFY) {
            return dslContext.selectCount().from(this).where(CLASSIFY_CODE.eq(classifyCode).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TClassify.T_CLASSIFY) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun update(dslContext: DSLContext, id: String, classifyRequest: ClassifyRequest) {
        with(TClassify.T_CLASSIFY) {
            dslContext.update(this)
                .set(CLASSIFY_CODE, classifyRequest.classifyCode)
                .set(CLASSIFY_NAME, classifyRequest.classifyName)
                .set(WEIGHT, classifyRequest.weight)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getClassify(dslContext: DSLContext, id: String): TClassifyRecord? {
        with(TClassify.T_CLASSIFY) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getClassifyByCode(dslContext: DSLContext, classifyCode: String, type: StoreTypeEnum): TClassifyRecord? {
        with(TClassify.T_CLASSIFY) {
            return dslContext.selectFrom(this)
                .where(CLASSIFY_CODE.eq(classifyCode))
                .and(TYPE.eq(type.type.toByte()))
                .fetchOne()
        }
    }

    fun getAllClassify(dslContext: DSLContext, type: Byte): Result<TClassifyRecord> {
        with(TClassify.T_CLASSIFY) {
            return dslContext
                .selectFrom(this)
                .where(TYPE.eq(type))
                .orderBy(WEIGHT.desc())
                .fetch()
        }
    }

    fun convert(record: TClassifyRecord): Classify {
        with(record) {
            // 分类信息名称没有配置国际化信息则取分类表里面的名称
            val classifyType = StoreTypeEnum.getStoreType(type.toInt())
            val classifyLanName = I18nUtil.getCodeLanMessage(
                messageCode = "$classifyType.classify.$classifyCode",
                defaultMessage = classifyName
            )
            return Classify(
                id = id,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                classifyType = classifyType,
                weight = weight,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
