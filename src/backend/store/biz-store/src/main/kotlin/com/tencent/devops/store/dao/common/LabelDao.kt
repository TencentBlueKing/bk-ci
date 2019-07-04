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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.records.TLabelRecord
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.LabelRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class LabelDao {

    fun add(dslContext: DSLContext, id: String, labelRequest: LabelRequest, type: Byte) {
        with(TLabel.T_LABEL) {
            dslContext.insertInto(
                this,
                ID,
                LABEL_CODE,
                LABEL_NAME,
                TYPE
            )
                .values(
                    id,
                    labelRequest.labelCode,
                    labelRequest.labelName,
                    type
                ).execute()
        }
    }

    fun countByName(dslContext: DSLContext, labelName: String, type: Byte): Int {
        with(TLabel.T_LABEL) {
            return dslContext.selectCount().from(this).where(LABEL_NAME.eq(labelName).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countByCode(dslContext: DSLContext, labelCode: String, type: Byte): Int {
        with(TLabel.T_LABEL) {
            return dslContext.selectCount().from(this).where(LABEL_CODE.eq(labelCode).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TLabel.T_LABEL) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun update(dslContext: DSLContext, id: String, labelRequest: LabelRequest) {
        with(TLabel.T_LABEL) {
            dslContext.update(this)
                .set(LABEL_CODE, labelRequest.labelCode)
                .set(LABEL_NAME, labelRequest.labelName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getLabel(dslContext: DSLContext, id: String): TLabelRecord? {
        with(TLabel.T_LABEL) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getAllLabel(dslContext: DSLContext, type: Byte): Result<TLabelRecord>? {
        with(TLabel.T_LABEL) {
            return dslContext
                .selectFrom(this)
                .where(TYPE.eq(type))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun convert(record: TLabelRecord): Label {
        with(record) {
            return Label(
                id = id,
                labelCode = labelCode,
                labelName = labelName,
                labelType = StoreTypeEnum.getStoreType(type.toInt()),
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}