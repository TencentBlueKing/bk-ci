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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.VMType
import com.tencent.devops.model.dispatch.tables.TDispatchVmType
import com.tencent.devops.model.dispatch.tables.records.TDispatchVmTypeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class VMTypeDao {

    fun findVMTypeById(dslContext: DSLContext, id: Int): TDispatchVmTypeRecord? {
        return with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            dslContext.selectFrom(this)
                    .where(TYPE_ID.eq(id))
                    .fetchAny()
        }
    }

    fun findAllVMType(dslContext: DSLContext): Result<TDispatchVmTypeRecord>? {
        return with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            dslContext.selectFrom(this)
                    .orderBy(TYPE_ID.asc())
                    .fetch()
        }
    }

    fun countByName(dslContext: DSLContext, typeName: String): Int {
        with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            return dslContext.selectCount().from(this).where(TYPE_NAME.eq(typeName)).fetchOne(0, Int::class.java)
        }
    }

    fun createVMType(
        dslContext: DSLContext,
        typeName: String
    ): Boolean {
        with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this)
                    .columns(TYPE_NAME, TYPE_CREATED_TIME, TYPE_UPDATED_TIME)
                    .values(typeName, now, now)
                    .execute() == 1
        }
    }

    fun updateVMType(
        dslContext: DSLContext,
        typeId: Int,
        typeName: String
    ): Boolean {
        with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            return dslContext.update(this)
                    .set(TYPE_NAME, typeName)
                    .set(TYPE_UPDATED_TIME, LocalDateTime.now())
                    .where(TYPE_ID.eq(typeId))
                    .execute() == 1
        }
    }

    fun deleteVMType(
        dslContext: DSLContext,
        typeId: Int
    ) {
        with(TDispatchVmType.T_DISPATCH_VM_TYPE) {
            dslContext.delete(this)
                    .where(TYPE_ID.eq(typeId))
                    .execute()
        }
    }

    fun parseVMType(record: TDispatchVmTypeRecord?): VMType? {
        return if (record == null) {
            null
        } else {
            VMType(record.typeId,
                    record.typeName,
                    record.typeCreatedTime.timestamp(),
                    record.typeUpdatedTime.timestamp())
        }
    }
}