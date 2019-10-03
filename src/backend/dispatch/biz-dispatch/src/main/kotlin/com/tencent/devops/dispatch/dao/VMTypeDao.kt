package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.VMType
import com.tencent.devops.model.dispatch.tables.TDispatchVmType
import com.tencent.devops.model.dispatch.tables.records.TDispatchVmTypeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Created by rdeng on 2017/9/1.
 */
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