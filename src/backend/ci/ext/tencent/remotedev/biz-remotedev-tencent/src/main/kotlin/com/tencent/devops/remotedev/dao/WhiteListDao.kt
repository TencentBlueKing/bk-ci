package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWhiteList
import com.tencent.devops.model.remotedev.tables.records.TWhiteListRecord
import com.tencent.devops.remotedev.pojo.WhiteList
import com.tencent.devops.remotedev.pojo.WhiteListType
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class WhiteListDao {

    fun add(
        dslContext: DSLContext,
        limit: WhiteList
    ): Int {
        return with(TWhiteList.T_WHITE_LIST) {
            dslContext.insertInto(
                this,
                NAME,
                TYPE,
                WINDOWS_GPU_LIMIT
            ).values(
                limit.name,
                limit.type.name,
                limit.windowsGpuLimit
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun addOrUpdate(
        dslContext: DSLContext,
        limit: WhiteList
    ): Int {
        return with(TWhiteList.T_WHITE_LIST) {
            dslContext.insertInto(
                this,
                NAME,
                TYPE,
                WINDOWS_GPU_LIMIT
            ).values(
                limit.name,
                limit.type.name,
                limit.windowsGpuLimit
            ).onDuplicateKeyUpdate()
                .set(WINDOWS_GPU_LIMIT, limit.windowsGpuLimit)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        name: String,
        type: WhiteListType
    ): WhiteList? {
        with(TWhiteList.T_WHITE_LIST) {
            return dslContext.selectFrom(this).where(TYPE.eq(type.name).and(NAME.eq(name))).fetchAny(mapper)
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Int {
        with(TWhiteList.T_WHITE_LIST) {
            return dslContext.delete(this).where(ID.eq(id)).limit(1).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        name: String,
        type: WhiteListType
    ): Int {
        with(TWhiteList.T_WHITE_LIST) {
            return dslContext.delete(this).where(NAME.eq(name).and(TYPE.eq(type.name))).limit(1).execute()
        }
    }

    class TWhiteListRecordJooqMapper : RecordMapper<TWhiteListRecord, WhiteList> {
        override fun map(record: TWhiteListRecord?): WhiteList? {
            return record?.run {
                WhiteList(
                    name = name,
                    type = WhiteListType.valueOf(type),
                    windowsGpuLimit = windowsGpuLimit
                )
            }
        }
    }

    companion object {
        val mapper = TWhiteListRecordJooqMapper()
    }
}
